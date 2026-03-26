# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - Unreleased

### Added
- `halogen-core`: HalogenThemeSpec, HalogenLlmProvider interface, HalogenColorScheme with 49 M3 color roles
- `halogen-core`: Pure Kotlin HCT color science (CAM16, tonal palettes, WCAG contrast validation)
- `halogen-core`: HalogenTypography (15 M3 text styles) and HalogenShapes (5 M3 shape sizes)
- `halogen-engine`: HalogenEngine with two-level cache (in-memory LRU + Room/SQLite)
- `halogen-engine`: Halogen.Builder for configuration, provider chaining with fallback
- `halogen-engine`: Prompt construction with system prompt, few-shot examples, and extension injection
- `halogen-compose`: HalogenTheme composable wrapping MaterialTheme
- `halogen-compose`: HalogenSettingsCard drop-in settings UI component
- `halogen-compose`: LocalHalogenEngine and LocalHalogenExtensions composition locals
- `halogen-provider-nano`: GeminiNanoProvider for on-device inference via ML Kit
- Custom extensions system for developer-defined theme tokens
- Keyed theme cache with eviction, refresh, and observability APIs
- One LLM call generates both light and dark color schemes
- KMP support: Android, iOS (arm64, simulatorArm64), Desktop (JVM), Web (WasmJs)
