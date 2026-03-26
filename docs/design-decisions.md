# Design Decisions

Architectural Decision Records (ADRs) explaining why Halogen is built the way it is.

---

## ADR-1: Pure Kotlin Color Science over material-color-utilities

### Context

Google's `material-color-utilities` library provides HCT color space, tonal palettes, and the full Material 3 color derivation pipeline. However, it is a JVM-only Java library. Halogen targets Android, iOS, Desktop, and Web via Kotlin Multiplatform.

### Options

1. **Depend on `material-color-utilities`** — Use Google's library directly. Only works on JVM targets (Android + Desktop). iOS and Web would need a separate implementation or be unsupported.
2. **Port to pure Kotlin** — Reimplement the core color math (CAM16, HCT, tonal palettes) in Kotlin `commonMain`. Works on all KMP targets.
3. **Use `expect`/`actual`** — Google's library on JVM, a separate port on native/wasm. Doubles maintenance burden.

### Decision

**Port to pure Kotlin.** The relevant math is well-documented (CAM16 is an international standard) and the implementation is approximately 500 lines of code. The color science is the heart of the library — it should work identically on every platform without expect/actual splits.

### Consequences

- All KMP targets share the same color expansion code
- No JVM-only dependency in the critical path
- Slightly more maintenance burden if Google updates their algorithm (unlikely — HCT is stable)
- Smaller dependency footprint for consumers

---

## ADR-2: LLM as Pluggable Provider, Not Core Dependency

### Context

Halogen generates themes using an LLM, but the library should work with any LLM — Gemini Nano, OpenAI, Claude, Ollama, or a custom model. Baking a specific LLM SDK into the core would force all consumers to pull that dependency.

### Options

1. **Bundle Gemini Nano in core** — Every consumer gets on-device inference. But iOS/Desktop/Web users pull an Android-only dependency they can't use.
2. **Pluggable interface** — Define `HalogenLlmProvider` in core with zero LLM imports. Ship Nano as a separate artifact.
3. **No LLM integration** — Just provide the theme expansion pipeline. Let developers handle prompt engineering themselves.

### Decision

**Pluggable interface.** `halogen-core` defines `HalogenLlmProvider` as a simple two-method interface. `halogen-provider-nano` implements it for Gemini Nano. Developers implement it for their preferred cloud LLM. The engine chains multiple providers with automatic failover.

### Consequences

- `halogen-core` has zero LLM dependencies
- A cloud-only developer never pulls ML Kit
- Provider implementation is trivial (~20 lines for most cloud APIs)
- The library cannot guarantee LLM quality — different models produce different results
- Fallback chaining handles device/platform heterogeneity

---

## ADR-3: Seed Colors + Expansion over Full Palette Generation

### Context

A full Material 3 `ColorScheme` has 49 color roles. The LLM could generate all 49, or it could generate a small number of seed values that the library expands.

### Options

1. **Generate all 49 colors** — The LLM outputs every M3 color role directly. Maximum creative control, but ~300+ output tokens, risk of inconsistent tonal relationships, and high chance of invalid output from smaller models.
2. **Generate 6 seeds + hints** — The LLM outputs 6 hex colors plus typography/shape hints (~12 fields, ~70 tokens). The library expands seeds into 49 roles using HCT tonal palettes.
3. **Generate 3 seeds** — Only primary, secondary, tertiary. Minimal token cost but the LLM can't influence neutrals, error color, or typography.

### Decision

**6 seeds + hints.** The LLM generates `primary`, `secondary`, `tertiary`, `neutralLight`, `neutralDark`, and `error` — enough to define the theme's identity and both light/dark neutral bases. Typography and shape hints (4 fields) let the LLM influence the full M3 surface without generating every value.

### Consequences

- Output is ~70 tokens — well within Gemini Nano's 256-token limit
- Tonal relationships are guaranteed correct by the HCT expansion pipeline
- The LLM focuses on creative decisions (hue, mood, character) rather than mechanical mapping
- Works reliably with small on-device models (Nano) and large cloud models alike
- Light and dark themes are always harmonious — same seeds, different tone mapping

---

## ADR-4: One LLM Call = Both Light and Dark Themes

### Context

Users expect system dark mode to work. A theme library needs both light and dark color schemes.

### Options

1. **Two LLM calls** — One for light, one for dark. Doubles latency, token cost, and cache entries. Risk of incoherent theme pairs.
2. **One call with `isDark` parameter** — LLM generates one mode per call. Themes are cached separately. Dark mode toggle requires a cache lookup.
3. **One call, dual neutrals** — LLM generates shared seeds (primary, secondary, tertiary, error) plus two neutral seeds (`neutralLight`, `neutralDark`). Library expands both palettes from the same seeds.

### Decision

**One call, dual neutrals.** The `HalogenThemeSpec` includes `neuL` and `neuD` — the only fields that differ between modes. Primary, secondary, tertiary, and error seeds are shared because their hue and chroma define the theme identity; only the tone changes between light and dark.

### Consequences

- Single LLM call produces a complete theme identity
- System dark mode toggle is instant — no second LLM call, no cache lookup
- Both schemes are guaranteed harmonious (same hues, different tones)
- Token cost is +1 field (~15 tokens) over a single-mode approach
- Cache stores one entry per key, not two

---

## ADR-5: Keyed Theme Cache as First-Class Primitive

### Context

Theme generation is expensive (1-3 seconds for on-device, network latency for cloud). Themes should be generated once and reused.

### Options

1. **No caching** — Regenerate on every call. Simple but slow and wasteful.
2. **Single active theme** — Cache the current theme only. Settings screen replaces it.
3. **Keyed cache** — Any string maps to a cached theme. Multiple themes coexist. The engine provides in-memory LRU caching by default, with optional persistent caching via a separate module.

### Decision

**Keyed cache with optional persistence.** Every `resolve(key, hint)` call uses the key as a cache identifier. Multiple themes coexist — a user can have different themes for different contexts (routes, categories, brands) and switch between them instantly. The engine ships with `MemoryThemeCache` (in-memory LRU) by default. Persistent caching via Room/SQLite is available as an optional module (`halogen-cache-room`) for consumers who need themes to survive process death.

### Consequences

- First resolve per key is slow (LLM call). Every subsequent resolve is instant (cache hit).
- In-memory LRU is the default — no database dependencies in `halogen-engine`
- Persistent caching (Room/SQLite) is opt-in via `halogen-cache-room` — consumers choose whether to include it
- Without `halogen-cache-room`, themes are lost on process death and must be re-generated
- Developers control the key namespace — they decide what constitutes a "context"
- Memory LRU prevents unbounded memory growth
- Room cache can be configured with `maxEntries` and `maxAge` for eviction control

---

## ADR-6: Wrapping MaterialTheme Instead of Replacing It

### Context

`HalogenTheme` needs to apply generated colors, typography, and shapes to the Compose tree. It could replace `MaterialTheme` entirely or wrap it.

### Options

1. **Replace MaterialTheme** — Provide a completely custom theme system. Breaks compatibility with M3 components that read from `MaterialTheme`.
2. **Wrap MaterialTheme** — `HalogenTheme` expands the spec and passes values to `MaterialTheme`. All standard M3 accessors (`MaterialTheme.colorScheme.primary`, etc.) work unchanged.
3. **Parallel system** — Both `MaterialTheme` and `HalogenTheme` exist independently. Developers choose which to use.

### Decision

**Wrap MaterialTheme.** `HalogenTheme` is a composable that expands `HalogenThemeSpec` into `ColorScheme`, `Typography`, and `Shapes`, then passes them to `MaterialTheme`. All existing M3 components and theme accessors work as expected.

### Consequences

- Zero migration cost — drop `HalogenTheme` around your existing `MaterialTheme` usage
- All M3 components pick up the generated theme automatically
- Standard accessors (`MaterialTheme.colorScheme.primary`) work unchanged
- Custom extensions are provided via a separate `LocalHalogenExtensions` composition local
- No reinvention of the theming wheel — Halogen adds generation, not a new theme system
