# Halogen

**Runtime theme generation for [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/)**

On-device with [Gemini Nano](https://developer.android.com/ai/gemini-nano) on Android. Cloud-powered everywhere else. Just a Gradle dependency.

---

## Quick Start

```kotlin
val halogen = Halogen.Builder()
    .provider(GeminiNanoProvider())
    .cache(HalogenCache.memory())
    .build()

halogen.resolve(key = "coffee", hint = "warm coffee shop vibes")

HalogenTheme { App() }
```

That's it. Halogen generates a full Material 3 theme from a natural language prompt - colors, typography, and shapes - cached locally so the LLM is called once per key, ever.

---

## Features

- **Pluggable LLM providers** - Ship with Gemini Nano on-device, or use OpenAI/Claude for cloud, or write your own. Set one provider per engine instance.
- **Keyed contextual themes** - Any string maps to a cached theme. A subreddit, a route, a category, a brand - each gets its own generated look and feel.
- **Full Material 3 coverage** - 49 color roles, 15 typography styles, 5 shape sizes. The LLM generates 6 seeds + hints; the library expands them using [HCT](https://material.io/blog/science-of-color-design) tonal palettes.
- **In-memory caching** - LRU cache with configurable max entries. Themes resolve in nanoseconds after first generation. The LLM is never called twice for the same key. Optional persistent caching via `halogen-cache-room` (backed by [Room KMP](https://developer.android.com/kotlin/multiplatform/room)) survives process death and app restarts.
- **Kotlin Multiplatform** - Android, iOS, Desktop (JVM), and Web (WasmJs). Gemini Nano is Android-only; all other platforms use cloud providers.
- **[WCAG AA](https://www.w3.org/WAI/WCAG21/Understanding/contrast-minimum.html) contrast validation** - Generated color schemes are validated for accessibility. Primary/secondary/tertiary contrast against their on-color counterparts meets minimum ratios.

---

## Platform Support

| Platform | LLM Provider | Cache | Room Cache | Compose UI |
|----------|-------------|-------|------------|------------|
| Android  | Gemini Nano (on-device) or cloud | Memory (LRU) | Yes (optional) | Material 3 |
| iOS      | Cloud providers | Memory (LRU) | Yes (optional) | Material 3 |
| Desktop (JVM) | Cloud providers | Memory (LRU) | Yes (optional) | Material 3 |
| Web (WasmJs) | Cloud providers | Memory (LRU) / localStorage | -- | Material 3 |

---

## Artifacts

```kotlin
// Core primitives — no LLM dependencies
implementation("me.mmckenna.halogen:halogen-core:0.1.0")

// Compose Multiplatform UI components
implementation("me.mmckenna.halogen:halogen-compose:0.1.0")

// Engine — caching, orchestration, prompt construction
implementation("me.mmckenna.halogen:halogen-engine:0.1.0")

// Gemini Nano provider (Android only)
implementation("me.mmckenna.halogen:halogen-provider-nano:0.1.0")

// Optional: Persistent theme cache backed by Room KMP (survives app restarts)
implementation("me.mmckenna.halogen:halogen-cache-room:0.1.0")
```

---

## Learn More

- [Quick Start](quick-start.md) - Add Halogen to your project in 5 minutes
- [Architecture](architecture.md) - How the resolve chain, caching, and color science work
- [Provider Guide](provider-guide.md) - Implement your own LLM provider
- [Custom Extensions](custom-extensions.md) - Add custom theme tokens
- [API Reference](api-reference.md) - Full API surface for all modules
- [Design Decisions](design-decisions.md) - Why Halogen is built the way it is
