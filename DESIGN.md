# Design Decisions

This document explains the non-obvious architectural decisions in Halogen. Each follows the format: context, decision, consequences.

For the full design specification, see [PLAN.md](PLAN.md).

---

## 1. Seeds Over Full Palettes

**Context:** Material 3 has 49 color roles per scheme (98 total for light + dark). We need the LLM to produce the right colors for a theme, but Gemini Nano has a ~256 output token limit and smaller models are less reliable at generating large structured outputs.

**Decision:** The LLM generates only 6 seed colors + 6 hints (~12 JSON fields, ~70 tokens). The library expands seeds into the full 49-role palette using HCT tonal palette math.

**Consequences:**
- Output stays under 256 tokens even with 10 custom extensions
- Works with weaker/smaller LLMs that struggle with large outputs
- Color harmony is guaranteed by the tonal palette algorithm rather than relying on the LLM to pick 49 coordinated colors
- One LLM call produces both light and dark themes (same seeds, different tone mappings)
- Trade-off: developers cannot override individual color roles from the LLM — they control seeds and the expansion config

---

## 2. Wrapping MaterialTheme, Not Replacing It

**Context:** Halogen generates theme data that needs to be applied to Compose UI. We could either build a custom design system or wrap the existing Material 3 `MaterialTheme`.

**Decision:** `HalogenTheme` composable calls `MaterialTheme` internally. Every existing M3 component works without modification. A `themeWrapper` overload allows non-M3 usage.

**Consequences:**
- Migration is one line of code — wrap your existing `MaterialTheme` with `HalogenTheme`
- Every M3 component (`Button`, `Card`, `TextField`, etc.) just works
- Developers keep access to `MaterialTheme.colorScheme`, `MaterialTheme.typography`, `MaterialTheme.shapes`
- Custom theme systems are supported via the `themeWrapper` parameter or by consuming `ExpandedTheme` directly from `halogen-core`
- Trade-off: the default path is coupled to M3, but the core data types (`HalogenColorScheme`, `HalogenTypography`, `HalogenShapes`) are M3-agnostic

---

## 3. Pluggable LLM Provider

**Context:** Gemini Nano is Android-only (Pixel 9+, Samsung S24+). Halogen is Kotlin Multiplatform. We need LLM inference on iOS, Desktop, and Web too.

**Decision:** `HalogenLlmProvider` is a simple interface in `halogen-core` with zero LLM SDK dependencies. Gemini Nano is shipped as a separate artifact (`halogen-provider-nano`). Developers implement the interface for any cloud provider.

**Consequences:**
- `halogen-core` and `halogen-engine` have zero LLM imports — pure Kotlin
- A developer using only OpenAI never pulls ML Kit
- Provider chaining with fallback: try Nano first, fall back to cloud
- Any LLM works — OpenAI, Claude, Gemini Cloud, Ollama, custom endpoints
- Trade-off: no built-in cloud provider — developers bring their own (typically 20-30 lines of code)

---

## 4. Keyed Theme Cache

**Context:** LLM inference is slow (~1-3 seconds). Most apps have repeating contexts (screens, categories, brands) that should reuse previously generated themes.

**Decision:** Themes are stored by string key — any developer-defined identifier (a route, a subreddit, a category, a brand name). The resolve chain is: memory cache -> server provider -> LLM -> default. In-memory LRU cache with configurable max entries (default 20).

**Consequences:**
- The LLM is called once per key, ever (until explicitly evicted)
- Hot themes resolve in nanoseconds (HashMap lookup)
- Contextual theming is a first-class feature — different parts of an app naturally get different themes
- Server-provided themes take priority over LLM-generated ones, enabling brand control
- Cache is observable via `StateFlow<CacheStats>` and `Flow<CacheEvent>`
- Trade-off: memory grows with unique keys. LRU eviction handles this, but developers should be aware of the cache size for apps with many unique keys.

---

## 5. Pure Kotlin Color Science

**Context:** Material 3's color system uses the HCT (Hue-Chroma-Tone) color space. Google publishes this as `material-color-utilities` in Java — but that's Android-only and blocks KMP.

**Options considered:**
- **Depend on `material-color-utilities` (Java):** Zero effort, but Android-only. Kills KMP.
- **`expect/actual` wrapper:** Still need to solve iOS/Web. Multiple implementations to maintain.
- **Pure Kotlin in `commonMain`:** ~450 lines of math. Runs everywhere. Single implementation.

**Decision:** Port the color science to pure Kotlin. CAM16 color appearance model, HCT conversion, tonal palette generation, and M3 scheme mapping — all in `commonMain`.

**Consequences:**
- Enables KMP with zero `expect/actual` for color math
- Single implementation, fully testable in `commonTest` against reference outputs
- No transitive Java dependency for any platform
- Maintenance is minimal — HCT and M3 tone mapping are stable specs
- ~450 lines total: HCT/sRGB conversion (~200), TonalPalette (~100), DynamicScheme mapping (~150)
- Validated against Google's Java implementation with exact hex-value assertions

---

## 6. Compact JSON Schema

**Context:** LLM token limits are real constraints. Gemini Nano recommends ~256 output tokens. Every character in the JSON response counts.

**Decision:** Use abbreviated `@SerialName` keys (`pri` instead of `primary`, `neuL` instead of `neutralLight`, `hw` instead of `headingWeight`, etc.) to minimize output tokens.

**Consequences:**
- Baseline output is ~65-80 tokens — well under the 256 limit
- Room for 10+ custom extensions without hitting limits
- Works with weaker models that have shorter context windows
- Trade-off: raw JSON is less human-readable, but developers never see it — `HalogenThemeSpec` has descriptive Kotlin property names
- The spec uses `ignoreUnknownKeys = true` and `isLenient = true` for resilient parsing of LLM output
