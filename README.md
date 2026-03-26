# Halogen

**LLM-generated themes for Compose Multiplatform**

On-device with Gemini Nano on Android. Cloud-powered everywhere else. Just a Gradle dependency.

[![CI](https://github.com/himattm/halogen/actions/workflows/ci.yml/badge.svg)](https://github.com/himattm/halogen/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/me.mmckenna.halogen/halogen-core)](https://central.sonatype.com/namespace/me.mmckenna.halogen)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![API Docs](https://img.shields.io/badge/API-Dokka-blue)](https://halogen.mmckenna.me/api/)

---

## The Problem

Theming in Compose is static. You pick colors at build time, ship them, and every user sees the same palette. Want user-personalized themes? You'd need to build a color picker, handle color harmony, generate light/dark variants, validate contrast ratios, and cache results. That's a lot of work for "let the user pick a vibe."

## The Solution

Halogen takes a natural language prompt -- "ocean vibes", "warm coffee shop", "neon cyberpunk" -- and generates a complete, accessible theme at runtime. The LLM produces 6 seed colors; the library expands them into 49 color roles, 15 typography styles, and 5 shape sizes using Material 3 color science. Results are cached so the LLM is called once per key, ever.

Works with Material 3 out of the box, or bring your own design system.

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

That's it. Halogen generates a full Material 3 theme from a natural language prompt -- colors, typography, and shapes -- cached locally so the LLM is called once per key, ever.

---

## Features

- **Pluggable LLM providers** -- Ship with Gemini Nano on-device, add OpenAI/Claude as a fallback, or write your own. The engine chains providers with automatic failover.
- **Keyed contextual themes** -- Any string maps to a cached theme. A subreddit, a route, a category, a brand -- each gets its own generated look and feel.
- **Full Material 3 coverage** -- 49 color roles, 15 typography styles, 5 shape sizes. The LLM generates 6 seeds + hints; the library expands them using HCT tonal palettes.
- **In-memory caching** -- LRU cache with configurable max entries. Themes resolve in nanoseconds after first generation. The LLM is never called twice for the same key.
- **Kotlin Multiplatform** -- Android, iOS, Desktop (JVM), and Web (WasmJs). Gemini Nano is Android-only; all other platforms use cloud providers.
- **WCAG AA contrast validation** -- Generated color schemes are validated for accessibility. Primary/secondary/tertiary contrast against their on-color counterparts meets minimum ratios.

---

## Configuration

Halogen ships with preset color science configurations that control how LLM seed colors are expanded into Material 3 palettes:

| Preset | Style |
|--------|-------|
| `HalogenConfig.Default` | M3 SchemeTonalSpot -- balanced, recommended for most apps |
| `HalogenConfig.Vibrant` | Bolder, more saturated colors |
| `HalogenConfig.Muted` | Desaturated, corporate, calm |
| `HalogenConfig.Monochrome` | Single-hue variations |
| `HalogenConfig.Punchy` | High-energy, high-contrast |
| `HalogenConfig.Pastel` | Soft, light, airy |
| `HalogenConfig.Editorial` | One strong primary, neutral everything else |
| `HalogenConfig.Expressive` | Colorful and fun, even neutrals are tinted |

```kotlin
val engine = Halogen.Builder()
    .provider(myProvider)
    .config(HalogenConfig.Vibrant)
    .build()
```

All presets are available via `HalogenConfig.presets` for building UI pickers.

---

## Platform Support

| Platform | LLM Provider | Cache | Compose UI |
|----------|-------------|-------|------------|
| Android  | Gemini Nano (on-device) + cloud fallback | Memory (LRU) | Material 3 |
| iOS      | Cloud providers | Memory (LRU) | Material 3 |
| Desktop (JVM) | Cloud providers | Memory (LRU) | Material 3 |
| Web (WasmJs) | Cloud providers | Memory (LRU) / localStorage | Material 3 |

---

## Installation

```kotlin
// Core primitives -- no LLM dependencies
implementation("me.mmckenna.halogen:halogen-core:0.1.0")

// Compose Multiplatform UI components
implementation("me.mmckenna.halogen:halogen-compose:0.1.0")

// Engine -- caching, orchestration, prompt construction
implementation("me.mmckenna.halogen:halogen-engine:0.1.0")

// Gemini Nano provider (Android only)
implementation("me.mmckenna.halogen:halogen-provider-nano:0.1.0")
```

---

## Architecture

```
engine.resolve(key = "coffee", hint = "warm coffee shop")
        |
        v
  [Cache: Memory LRU] --HIT--> return (nanoseconds)
        |MISS
        v
  [ServerProvider?] ----HIT--> write cache, return
        |MISS
        v
  [LLM Provider] --> parse JSON --> expand 6 seeds to 49 colors
        |                           via HCT tonal palettes
        v
  [Write cache] --> [HalogenTheme recomposes UI]
```

One LLM call produces both light and dark color schemes. The system dark mode toggle switches between them instantly.

---

## Custom Theme Systems

Halogen's core is M3-agnostic. `HalogenColorScheme` is just ARGB integers, `HalogenTypography` is font weights, `HalogenShapes` is dp values. Use Material 3 out of the box, or map to your own design system:

```kotlin
HalogenTheme(
    spec = currentSpec,
    themeWrapper = { expanded, isDark, content ->
        val myTheme = expanded.toMyCompanyTheme(isDark)
        CompositionLocalProvider(LocalMyTheme provides myTheme) {
            content()
        }
    },
) { App() }
```

See the [Custom Theme Systems](https://halogen.mmckenna.me/custom-theme-systems/) guide for a full walkthrough.

---

## Compatibility

| Dependency | Version |
|-----------|---------|
| Kotlin | 2.2.20 |
| Compose Multiplatform | 1.10.1 |
| Android Gradle Plugin | 8.13.2 |
| Min Android SDK | 24 (Nano provider: 26) |
| JVM Target | 17 |

---

## Documentation

Full documentation is available at [halogen.mmckenna.me](https://halogen.mmckenna.me):

- [Quick Start](https://halogen.mmckenna.me/quick-start/) -- Add Halogen to your project in 5 minutes
- [Architecture](https://halogen.mmckenna.me/architecture/) -- How the resolve chain, caching, and color science work
- [Provider Guide](https://halogen.mmckenna.me/provider-guide/) -- Implement your own LLM provider
- [Custom Extensions](https://halogen.mmckenna.me/custom-extensions/) -- Add custom theme tokens
- [Custom Theme Systems](https://halogen.mmckenna.me/custom-theme-systems/) -- Use Halogen with non-M3 design systems
- [API Reference](https://halogen.mmckenna.me/api-reference/) -- Full API surface for all modules
- [Use Cases](https://halogen.mmckenna.me/use-cases/) -- Common patterns and real-world examples
- [Design Decisions](https://halogen.mmckenna.me/design-decisions/) -- Why Halogen is built the way it is

---

## Contributing

Contributions are welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) for development setup, code style, and PR guidelines.

---

## License

```
Copyright 2025 Matt McKenna

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
