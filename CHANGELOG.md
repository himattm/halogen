# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.2.0] - Unreleased

### Added
- `halogen-image`: Image-to-theme color extraction using histogram bucketing and HCT k-means clustering
- `halogen-image`: `resolveImage()` engine extensions for URL-based and raw-pixel theme resolution
- `halogen-image`: `extractColors()` for standalone palette extraction without theme resolution
- `halogen-image`: Coil 3 integration with platform-specific pixel extraction (Bitmap on Android, Skia on JVM/iOS/wasmJs)
- `halogen-image`: Algorithmic (`toSpec()`) and LLM-enhanced (`toHint()`) theme generation paths

## [0.1.0] - Unreleased

### Added
- `halogen-core`: HalogenThemeSpec, HalogenLlmProvider interface, HalogenColorScheme with 49 M3 color roles
- `halogen-core`: Pure Kotlin HCT color science (CAM16, tonal palettes, WCAG contrast validation)
- `halogen-core`: HalogenTypography (15 M3 text styles) and HalogenShapes (5 M3 shape sizes)
- `halogen-engine`: HalogenEngine with in-memory LRU cache, single provider model
- `halogen-cache-room`: Optional Room KMP persistent cache (Android, iOS, JVM)
- `halogen-engine`: Halogen.Builder for configuration with `.provider()` and `.remoteThemes()`
- `halogen-engine`: Prompt construction with system prompt, few-shot examples, and extension injection
- `halogen-compose`: HalogenTheme composable wrapping MaterialTheme
- `halogen-compose`: HalogenSettingsCard drop-in settings UI component
- `halogen-compose`: LocalHalogenExtensions composition local
- `halogen-provider-nano`: GeminiNanoProvider for on-device inference via ML Kit
- Custom extensions system for developer-defined theme tokens
- Keyed theme cache with eviction, refresh, and observability APIs
- One LLM call generates both light and dark color schemes
- KMP support: Android, iOS (arm64, simulatorArm64), Desktop (JVM), Web (WasmJs)
