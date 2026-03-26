# Halogen — LLM-Generated Theming for Compose Multiplatform

## The Pitch (Halo + Gen = Halogen)

Halogen is an open-source Kotlin Multiplatform library that lets users personalize your app's look and feel using natural language. On Android, it's powered by Gemini Nano on-device — no server, no API key, no network. On iOS, Desktop, and Web, it works with any cloud LLM provider. It transforms prompts like "ocean vibes, dark mode" or "warm and cozy, rounded corners" into a full Material 3 theme — colors, typography, and shapes — applied instantly at runtime across every platform Compose Multiplatform supports.

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Material 3 Theme Coverage](#2-material-3-theme-coverage)
3. [LLM Provider Abstraction](#3-llm-provider-abstraction)
4. [Token Budget Analysis](#4-token-budget-analysis)
5. [Schema Design](#5-schema-design)
6. [Prompt Engineering](#6-prompt-engineering)
7. [Library API Design](#7-library-api-design)
8. [Custom Extensions](#8-custom-extensions)
9. [Persistence & Keyed Theme Cache](#9-persistence--keyed-theme-cache)
10. [Accessibility & Safety](#10-accessibility--safety)
11. [Graceful Degradation](#11-graceful-degradation)
12. [Module Structure](#12-module-structure)
13. [Kotlin Multiplatform](#13-kotlin-multiplatform)
14. [Build Phases](#14-build-phases)
15. [Sample App — Habitat](#15-sample-app--habitat)
16. [Open Source Strategy](#16-open-source-strategy)

---

## 1. Architecture Overview

```
Developer calls:  engine.resolve(key = "coffee", hint = "coffee")
                          │
                          ▼
               ┌─────────────────────┐
               │  L1: Memory LRU    │──── HIT ──▶ Return (nanoseconds)
               └─────────┬──────────┘
                    MISS  │
                          ▼
               ┌─────────────────────┐
               │  L2: Room (SQLite)  │──── HIT ──▶ Promote to L1, return (~1-5ms)
               └─────────┬──────────┘
                    MISS  │
                          ▼
               ┌─────────────────────┐
               │  ServerProvider?    │──── HIT ──▶ Write L1 + L2, return
               └─────────┬──────────┘
                    MISS  │
                          ▼
┌─────────────────────────────────────────────────────┐
│                    HalogenEngine                     │
│                                                     │
│  1. Build prompt (system + few-shot + hint)          │
│  2. Call LLM provider (Nano, cloud, or chain)        │
│  3. Parse JSON response → HalogenThemeSpec           │
│  4. Validate colors (contrast, accessibility)        │
│  5. Write to L1 + L2 cache with key ("coffee")       │
│  6. Emit to StateFlow                                │
│                                                     │
└────────────────┬────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────┐
│            HalogenTheme (Composable)                 │
│                                                     │
│  MaterialTheme(                                      │
│    colorScheme = expandedColors.toM3(),               │
│    typography  = expandedTypography.toM3(),           │
│    shapes      = expandedShapes.toM3()               │
│  ) { content() }                                     │
│                                                     │
└─────────────────────────────────────────────────────┘
                 │
                 ▼
        Entire app/screen recomposes with new theme
```

Key principles:

1. **The LLM does NOT generate all 49 color roles.** It generates 5-7 seed values. The library expands them into the full M3 palette using tonal palette generation, keeping output compact.
2. **Themes are keyed with two-level caching.** Any string — a route, a category, a subreddit — maps to a cached theme. In-memory LRU (L1) sits in front of SQLite on disk (L2). Hot themes resolve in nanoseconds. Cold themes load from disk in ~1-5ms and promote to memory. Disk cache survives process death, app restarts, and device reboots. The LLM is called once per key, ever.
3. **Server themes take priority over LLM.** If you have a backend that defines themes, those are used first and cached locally. The LLM is the fallback for unknown contexts.
4. **The LLM is pluggable.** `HalogenLlmProvider` is an interface. Ship with Gemini Nano for on-device, add OpenAI/Claude as a fallback, or write your own. The engine chains providers with automatic failover.
5. **No hard dependency on any LLM.** `halogen-core` and `halogen-engine` have zero LLM imports. The Nano provider is a separate artifact (`halogen-provider-nano`). A developer using only cloud LLMs never pulls ML Kit.

---

## 2. Material 3 Theme Coverage

### 2.1 Color Scheme — All 49 Roles

The full Material 3 `ColorScheme` has 49 color properties. We group them by what the LLM needs to generate vs. what the library derives.

#### LLM Generates (Seed Colors — 6 values + hints)

| Seed          | M3 Concept    | Description                           |
|--------------|--------------|----------------------------------------|
| `primary`     | Primary key   | Brand color, main actions              |
| `secondary`   | Secondary key | Supporting elements                    |
| `tertiary`    | Tertiary key  | Contrasting accents                    |
| `neutralLight` | Neutral key (light) | Light mode backgrounds, surfaces |
| `neutralDark`  | Neutral key (dark)  | Dark mode backgrounds, surfaces  |
| `error`       | Error key     | Error states (usually red-ish)         |

One LLM call → both light and dark themes. The primary, secondary, tertiary, and error seeds are shared — their hue and chroma define the theme identity, and the tonal palette system maps them to the correct brightness for each mode. Only the neutral seeds differ, because light backgrounds and dark backgrounds need fundamentally different base tones.

#### Library Derives (Full 49-color palette)

**Primary group (5 + 4 fixed = 9)**
- `primary`, `onPrimary`, `primaryContainer`, `onPrimaryContainer`, `inversePrimary`
- `primaryFixed`, `primaryFixedDim`, `onPrimaryFixed`, `onPrimaryFixedVariant`

**Secondary group (5 + 4 fixed = 9)**
- `secondary`, `onSecondary`, `secondaryContainer`, `onSecondaryContainer`
- `secondaryFixed`, `secondaryFixedDim`, `onSecondaryFixed`, `onSecondaryFixedVariant`

**Tertiary group (5 + 4 fixed = 9)**
- `tertiary`, `onTertiary`, `tertiaryContainer`, `onTertiaryContainer`
- `tertiaryFixed`, `tertiaryFixedDim`, `onTertiaryFixed`, `onTertiaryFixedVariant`

**Error group (4)**
- `error`, `onError`, `errorContainer`, `onErrorContainer`

**Surface group (13)**
- `surface`, `onSurface`, `surfaceVariant`, `onSurfaceVariant`
- `surfaceTint`, `surfaceBright`, `surfaceDim`
- `surfaceContainer`, `surfaceContainerHigh`, `surfaceContainerHighest`
- `surfaceContainerLow`, `surfaceContainerLowest`
- `background`, `onBackground`

**Utility (5)**
- `inverseSurface`, `inverseOnSurface`
- `outline`, `outlineVariant`
- `scrim`

**Derivation strategy:** Use HCT (Hue-Chroma-Tone) color space to expand seed colors into tonal palettes. Given a seed color, generate a TonalPalette with 13 tone levels. Map each tone to the appropriate M3 role based on light/dark mode. See [Section 13.3 — The Color Science Decision](#133-the-color-science-decision) for why Halogen includes a pure Kotlin implementation of this math rather than depending on Google's Java `material-color-utilities` library.

### 2.2 Typography — All 15 Text Styles

M3 Typography has 5 categories × 3 sizes = 15 styles:

| Category  | Large          | Medium          | Small          |
|-----------|----------------|-----------------|----------------|
| Display   | displayLarge   | displayMedium   | displaySmall   |
| Headline  | headlineLarge  | headlineMedium  | headlineSmall  |
| Title     | titleLarge     | titleMedium     | titleSmall     |
| Body      | bodyLarge      | bodyMedium      | bodySmall      |
| Label     | labelLarge     | labelMedium     | labelSmall     |

#### LLM Generates (Typography Hints)

| Hint         | Description                         | Example Values          |
|-------------|-------------------------------------|--------------------------|
| `fontMood`   | Overall typographic feeling         | "modern", "classic", "playful", "minimal" |
| `headingWeight` | Weight for display/headline styles | 300-900               |
| `bodyWeight`  | Weight for body/label styles       | 300-700                |
| `letterSpacingTight` | Whether to tighten letter spacing | true/false        |

#### Library Derives

Maps `fontMood` to built-in font family suggestions:
- "modern" → sans-serif (default)
- "classic" → serif
- "playful" → rounded sans-serif
- "minimal" → thin sans-serif
- "monospace" → monospace

Generates all 15 TextStyle values with appropriate sizes (already defined in M3 spec), adjusted weight, and letter spacing. Developers can override the font family mapping with their own custom fonts.

### 2.3 Shapes — All 5 Size Categories

| Size       | Default Corner | M3 Usage                          |
|-----------|---------------|-----------------------------------|
| extraSmall | 4.dp          | Small buttons, chips              |
| small      | 8.dp          | Cards, small FABs                 |
| medium     | 16.dp         | Dialogs, navigation drawers       |
| large      | 24.dp         | Sheets, large FABs                |
| extraLarge | 32.dp         | Full-screen sheets                |

#### LLM Generates

| Hint          | Description                    | Example Values |
|--------------|--------------------------------|----------------|
| `cornerStyle` | Shape character                | "sharp", "rounded", "pill", "soft" |
| `cornerScale` | Multiplier on default radii    | 0.0 - 2.0     |

#### Library Derives

Maps `cornerStyle` to a base radius set, then scales by `cornerScale`:
- "sharp" → 0, 2, 4, 8, 12
- "rounded" → 4, 8, 16, 24, 32 (M3 default)
- "pill" → 8, 16, 24, 48, 64
- "soft" → 6, 12, 20, 28, 36

All shapes use `RoundedCornerShape` since that's what M3 components expect.

---

## 3. LLM Provider Abstraction

The LLM is pluggable. Halogen defines a single interface that any LLM — on-device or cloud — must implement. Gemini Nano is the first-party provider, but developers can bring their own.

### 3.1 The HalogenLlmProvider Interface

This lives in `halogen-core` so it has zero Android or LLM dependencies:

```kotlin
package halogen

/**
 * Interface for generating theme JSON from a natural language hint.
 * Implement this to connect any LLM — on-device or cloud.
 */
interface HalogenLlmProvider {

    /**
     * Generate a HalogenThemeSpec JSON string from a prompt.
     *
     * @param prompt  The fully constructed prompt (system + few-shot + user hint).
     *                The engine builds this; the provider just sends it.
     * @return Raw JSON string response from the LLM.
     * @throws HalogenLlmException on failure.
     */
    suspend fun generate(prompt: String): String

    /**
     * Check if this provider is currently available.
     * For Gemini Nano: checks AICore status.
     * For cloud providers: could check network or API key presence.
     */
    suspend fun availability(): HalogenLlmAvailability
}

enum class HalogenLlmAvailability {
    READY,           // Can generate right now
    INITIALIZING,    // Downloading model, warming up, etc.
    UNAVAILABLE      // Not supported on this device / no API key / no network
}

class HalogenLlmException(
    message: String,
    cause: Throwable? = null,
    val isRetryable: Boolean = false
) : Exception(message, cause)
```

The engine owns prompt construction (system prompt, few-shot examples, extensions). The provider is just a pipe — it receives a fully-formed prompt string and returns the raw LLM output. This keeps providers dead simple to implement.

### 3.2 First-Party Provider: Gemini Nano

Shipped as a separate artifact. Only pulls ML Kit as a dependency.

```kotlin
package halogen.provider.nano

class GeminiNanoProvider(
    private val context: Context,
    private val temperature: Float = 0.3f,
    private val topK: Int = 10,
) : HalogenLlmProvider {

    private val generativeModel by lazy { Generation.getClient() }

    override suspend fun generate(prompt: String): String {
        val response = generativeModel.generateContent(
            generateContentRequest(TextPart(prompt)) {
                temperature = this@GeminiNanoProvider.temperature
                topK = this@GeminiNanoProvider.topK
            }
        )
        return response.candidates.firstOrNull()?.text
            ?: throw HalogenLlmException("Empty response from Gemini Nano")
    }

    override suspend fun availability(): HalogenLlmAvailability {
        return when (generativeModel.checkStatus()) {
            FeatureStatus.AVAILABLE -> HalogenLlmAvailability.READY
            FeatureStatus.DOWNLOADABLE,
            FeatureStatus.DOWNLOADING -> HalogenLlmAvailability.INITIALIZING
            else -> HalogenLlmAvailability.UNAVAILABLE
        }
    }

    /** Trigger Nano model download if needed. */
    suspend fun ensureDownloaded(): Flow<DownloadStatus> {
        return generativeModel.download()
    }
}
```

### 3.3 Example: OpenAI Provider (Community / Developer-Implemented)

A developer who wants to support older devices or wants cloud power:

```kotlin
// Developer writes this in their app — not part of the Halogen library
class OpenAiProvider(
    private val apiKey: String,
    private val model: String = "gpt-4o-mini",
    private val client: OkHttpClient = OkHttpClient(),
) : HalogenLlmProvider {

    override suspend fun generate(prompt: String): String {
        val body = """
            {"model":"$model","messages":[{"role":"user","content":${prompt.toJsonString()}}],
             "temperature":0.3,"max_tokens":256}
        """.trimIndent()

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        return withContext(Dispatchers.IO) {
            val response = client.newCall(request).execute()
            val json = JSONObject(response.body!!.string())
            json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        }
    }

    override suspend fun availability(): HalogenLlmAvailability {
        return if (apiKey.isNotBlank()) HalogenLlmAvailability.READY
               else HalogenLlmAvailability.UNAVAILABLE
    }
}
```

### 3.4 Example: Claude Provider (Community / Developer-Implemented)

```kotlin
class ClaudeProvider(
    private val apiKey: String,
    private val model: String = "claude-sonnet-4-20250514",
) : HalogenLlmProvider {

    override suspend fun generate(prompt: String): String {
        // Similar to OpenAI — POST to Anthropic API, return content text
        // ...
    }

    override suspend fun availability() = HalogenLlmAvailability.READY
}
```

### 3.5 Chaining Providers (Fallback)

The engine supports a prioritized list of providers. If the first is unavailable or fails, it tries the next:

```kotlin
val halogen = Halogen.Builder(context)
    .provider(GeminiNanoProvider(context))                    // try on-device first
    .fallbackProvider(OpenAiProvider(apiKey = BuildConfig.OPENAI_KEY))  // cloud fallback
    .cache(HalogenCache.room(context))
    .build()
```

Resolve flow with fallback:

```
cache MISS → serverProvider MISS →
    provider[0] (Nano) → UNAVAILABLE →
    provider[1] (OpenAI) → generate → parse → cache → apply
```

### 3.6 Gemini Nano Constraints (Provider-Specific)

These constraints apply specifically to `GeminiNanoProvider`, not to the core library:

| Constraint                | Value                 | Mitigation                       |
|--------------------------|-----------------------|----------------------------------|
| Max input tokens          | ~4000 (~3000 words)  | Prompt is ~1100 tokens           |
| Max output tokens         | ~256 recommended     | JSON output is ~60-100 tokens    |
| Supported languages       | English, Korean       | Start English, expand later      |
| Foreground only           | Required             | Theming happens in foreground    |
| Inference quota           | Per-app limit         | Cache aggressively               |
| Battery quota             | Daily limit           | Themes are generated rarely      |
| Locked bootloader         | Required             | Fallback to cloud provider       |
| Device support            | Pixel 9+, Samsung S24+| Fallback to cloud provider      |

---

## 4. Token Budget Analysis

### Input Budget (~4000 tokens max)

| Section             | Estimated Tokens | Notes                          |
|--------------------|------------------|--------------------------------|
| System instructions | ~400             | Role, rules, output format     |
| Schema definition   | ~200             | JSON field descriptions         |
| Few-shot example 1  | ~150             | "ocean vibes" → JSON           |
| Few-shot example 2  | ~150             | "warm cozy" → JSON             |
| Few-shot example 3  | ~150             | "minimal dark" → JSON          |
| User request        | ~50-100          | "sunset colors, playful shapes"|
| **Total**           | **~1100-1150**   | **Well under 4000 limit**      |

### Output Budget (~256 tokens max)

```json
{
  "pri": "#1A73E8",
  "sec": "#34A853",
  "ter": "#FBBC04",
  "neuL": "#F8F9FA",
  "neuD": "#1C1B1F",
  "err": "#D93025",
  "font": "modern",
  "hw": 700,
  "bw": 400,
  "ls": false,
  "cs": "rounded",
  "cx": 1.0
}
```

**Token count: ~65-80 tokens.** One call produces both light and dark themes. Well under the 256-token limit even with 5-10 custom extensions.

---

## 5. Schema Design

### HalogenThemeSpec — The LLM Output Contract

Every LLM call produces **both a light and dark theme** from a single set of seeds. The tonal palette system handles light/dark tone mapping automatically — the same primary hue maps to tone 40 in light mode and tone 80 in dark mode. The only field that differs is the neutral seed, since light backgrounds want warm whites while dark backgrounds want cool charcoals.

```kotlin
@Serializable
data class HalogenThemeSpec(
    // Seed colors (hex strings) — hue and chroma matter, tone is derived
    @SerialName("pri") val primary: String,           // "#RRGGBB" — brand color
    @SerialName("sec") val secondary: String,          // supporting color
    @SerialName("ter") val tertiary: String,           // accent color
    @SerialName("neuL") val neutralLight: String,      // light mode neutral (warm white, cool gray, etc.)
    @SerialName("neuD") val neutralDark: String,       // dark mode neutral (charcoal, dark slate, etc.)
    @SerialName("err") val error: String,              // error color

    // Typography hints
    @SerialName("font") val fontMood: String,          // "modern"|"classic"|"playful"|"minimal"|"mono"
    @SerialName("hw") val headingWeight: Int,           // 100-900
    @SerialName("bw") val bodyWeight: Int,              // 100-900
    @SerialName("ls") val tightLetterSpacing: Boolean,

    // Shape hints
    @SerialName("cs") val cornerStyle: String,          // "sharp"|"rounded"|"pill"|"soft"
    @SerialName("cx") val cornerScale: Float,           // 0.0 - 2.0

    // Custom extensions (optional, developer-defined)
    @SerialName("ext") val extensions: Map<String, String>? = null
)
```

Note what changed from the obvious approach: instead of a `dark: Boolean` that forces the LLM to pick one mode, we always get both. The `isDark` field is gone. The LLM generates one theme identity (hues, typography, shapes) with two neutral seeds — one for each mode. The library expands both palettes and the system dark mode setting picks which one to use at runtime.

This means:
- **One LLM call = one complete theme identity** (light + dark)
- **System dark mode toggle works instantly** — no second LLM call, no cache miss
- **Both schemes are harmonious** — same hues, same character, just different brightness mapping
- **Token cost is +1 field** — `neuD` adds ~15 tokens to output, total still ~80 tokens

### Expansion Pipeline

```
HalogenThemeSpec (LLM output, ~12 fields)
         │
         ▼
    HalogenPalette
    ├── primaryTonalPalette     (13 tones — shared)
    ├── secondaryTonalPalette   (13 tones — shared)
    ├── tertiaryTonalPalette    (13 tones — shared)
    ├── neutralLightTonalPalette (13 tones — from neuL)
    ├── neutralDarkTonalPalette  (13 tones — from neuD)
    └── errorTonalPalette       (13 tones — shared)
         │
         ├──── expand(isDark = false) ──▶ HalogenColorScheme (light, 49 roles)
         │                                    └── toM3() → ColorScheme
         │
         └──── expand(isDark = true) ───▶ HalogenColorScheme (dark, 49 roles)
                                              └── toM3() → ColorScheme
         │
         ▼
    HalogenTypography (15 M3 text styles — shared across modes)
         │
         ├── toM3Typography() → Typography
         │
         ▼
    HalogenShapes (5 M3 shape sizes — shared across modes)
         │
         ├── toM3Shapes() → Shapes
         │
         ▼
    HalogenTheme composable picks light or dark based on isSystemInDarkTheme()
```

---

## 6. Prompt Engineering

### System Prompt

```
You are a UI theme designer. Given a user's description, output a JSON 
color theme that works in BOTH light and dark mode. Use ONLY this format:

{
  "pri": "#RRGGBB",    // primary brand color
  "sec": "#RRGGBB",    // secondary color
  "ter": "#RRGGBB",    // tertiary accent color
  "neuL": "#RRGGBB",   // light mode background tone (light: #Ex-#Fx range)
  "neuD": "#RRGGBB",   // dark mode background tone (dark: #1x-#2x range)
  "err": "#RRGGBB",    // error color
  "font": "modern",    // one of: modern, classic, playful, minimal, mono
  "hw": 700,           // heading font weight (100-900)
  "bw": 400,           // body font weight (100-900) 
  "ls": false,         // tight letter spacing
  "cs": "rounded",     // one of: sharp, rounded, pill, soft
  "cx": 1.0            // corner scale (0.0-2.0)
}

Rules:
- Output ONLY the JSON object, no other text
- All colors must be valid 6-digit hex with # prefix
- Choose colors that work harmoniously together
- Ensure primary and secondary are visually distinct
- neuL must be a light color suitable for light mode backgrounds
- neuD must be a dark color suitable for dark mode backgrounds
- neuL and neuD should feel like the same theme at different brightnesses
- err should always be readable (typically red-ish)
```

### Few-Shot Examples

```
User: ocean vibes, calming
{"pri":"#0077B6","sec":"#00B4D8","ter":"#90E0EF","neuL":"#F0F7FA","neuD":"#0A1929","err":"#D32F2F","font":"modern","hw":600,"bw":400,"ls":false,"cs":"soft","cx":1.2}

User: neon cyberpunk
{"pri":"#BB86FC","sec":"#03DAC6","ter":"#CF6679","neuL":"#F5F0FF","neuD":"#121212","err":"#CF6679","font":"mono","hw":700,"bw":400,"ls":true,"cs":"sharp","cx":0.5}

User: warm and cozy, rounded
{"pri":"#B85C38","sec":"#E0C097","ter":"#5C3D2E","neuL":"#FFF8F0","neuD":"#1F1410","err":"#C62828","font":"classic","hw":600,"bw":400,"ls":false,"cs":"pill","cx":1.5}
```

Note: the few-shot examples no longer have a `dark` field. Each example produces seed colors for both modes. "neon cyberpunk" gets a near-black `neuD` AND a subtle cool purple `neuL` — the LLM understands the theme identity applies to both modes.

### Prompt Construction at Runtime

```kotlin
fun buildPrompt(userRequest: String): String {
    return buildString {
        append(SYSTEM_PROMPT)
        append("\n\n")
        FEW_SHOT_EXAMPLES.forEach { (input, output) ->
            append("User: $input\n$output\n\n")
        }
        append("User: $userRequest\n")
    }
}
```

---

## 7. Library API Design

### 7.1 Initialization

```kotlin
// ── Basic: Gemini Nano on-device ──
val halogen = Halogen.Builder(context)
    .provider(GeminiNanoProvider(context))
    .defaultTheme(HalogenDefaults.materialYou())
    .cache(HalogenCache.room(context, maxEntries = 100))
    .build()

// ── With cloud fallback for unsupported devices ──
val halogen = Halogen.Builder(context)
    .provider(GeminiNanoProvider(context))
    .fallbackProvider(OpenAiProvider(apiKey = BuildConfig.OPENAI_KEY))
    .defaultTheme(HalogenDefaults.light())
    .cache(HalogenCache.room(context))
    .build()

// ── With server-provided themes + LLM fallback ──
val halogen = Halogen.Builder(context)
    .provider(GeminiNanoProvider(context))
    .serverProvider(MyApiThemeProvider(api))
    .cache(HalogenCache.room(context, maxEntries = 200))
    .build()

// ── Cloud-only (no Gemini Nano dependency at all) ──
val halogen = Halogen.Builder(context)
    .provider(ClaudeProvider(apiKey = BuildConfig.CLAUDE_KEY))
    .cache(HalogenCache.room(context))
    .build()

// Provide to the Compose tree
CompositionLocalProvider(
    LocalHalogenEngine provides halogen
) {
    HalogenTheme {
        // Global/default theme
        App()
    }
}
```

### 7.2 HalogenTheme Composable

```kotlin
@Composable
fun HalogenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val engine = LocalHalogenEngine.current
    val themeSpec by engine.activeTheme.collectAsState()
    
    // One HalogenThemeSpec → two ColorSchemes.
    // The same spec expands to both light and dark palettes.
    // darkTheme just picks which expansion to use.
    val colorScheme = remember(themeSpec, darkTheme) {
        themeSpec?.expandColors(isDark = darkTheme)?.toM3()
            ?: if (darkTheme) darkColorScheme() else lightColorScheme()
    }
    
    // Typography and shapes are mode-independent — shared across light/dark
    val typography = remember(themeSpec) {
        themeSpec?.expandTypography()?.toM3()
            ?: Typography()
    }
    
    val shapes = remember(themeSpec) {
        themeSpec?.expandShapes()?.toM3()
            ?: Shapes()
    }
    
    val extensions = remember(themeSpec) {
        themeSpec?.extensions?.let { HalogenExtensions(it) }
            ?: HalogenExtensions.empty()
    }
    
    CompositionLocalProvider(
        LocalHalogenExtensions provides extensions
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}
```

When the user toggles system dark mode, `isSystemInDarkTheme()` changes, `expandColors()` is re-called with the other mode, and the entire UI recomposes with the complementary palette — instantly, no LLM call, no cache lookup, just a different tone mapping from the same seeds.

### 7.3 Accessing Theme Values

```kotlin
// Standard M3 access still works perfectly
MaterialTheme.colorScheme.primary
MaterialTheme.typography.headlineMedium
MaterialTheme.shapes.medium

// Custom extensions via Halogen
HalogenTheme.extensions["success"]?.toColor()
HalogenTheme.extensions["brandGradientStart"]?.toColor()
```

### 7.4 Triggering Theme Changes

```kotlin
val engine = LocalHalogenEngine.current

// ── Global theme (settings screen, user preference) ──
val result = engine.resolve(
    key = "user_global",
    hint = "sunset colors with playful rounded shapes"
)

// ── Contextual theme (subreddit, category, brand) ──
val result = engine.resolve(
    key = "coffee",
    hint = "coffee, warm browns, cozy café"
    // hint is only used on cache miss — ignored if cached
)

// ── Handle result ──
when (result) {
    is HalogenResult.Success -> { /* generated by LLM, cached, applied */ }
    is HalogenResult.Cached  -> { /* loaded from cache, no LLM call */ }
    is HalogenResult.FromServer -> { /* fetched from server provider, cached */ }
    is HalogenResult.ParseError -> { /* JSON parsing failed */ }
    is HalogenResult.Unavailable -> { /* no provider available */ }
    is HalogenResult.QuotaExceeded -> { /* try again later */ }
}

// ── Other operations ──
engine.applyDefault()                       // revert to default theme
engine.regenerate("coffee", "coffee")       // force re-generate, bypass cache
engine.apply("coffee", serverProvidedSpec)  // inject a server-provided theme
engine.prefetch("ocean", "ocean")           // cache without applying

// ── Cache management ──
engine.cachingEnabled = false               // disable caching — every resolve hits LLM
engine.cachingEnabled = true                // re-enable — cached themes still there

engine.evict("coffee")                      // remove one key
engine.evict(setOf("coffee", "ocean"))      // remove multiple keys
engine.clearCache()                         // remove ALL cached themes
engine.evictOlderThan(30.days)              // remove stale themes
engine.evictBySource(ThemeSource.LLM)       // keep server themes, clear LLM-generated

engine.refresh("coffee")                    // evict + re-resolve a single key
engine.refreshAll(concurrency = 3)          // regenerate all cached themes (parallel)

// ── Cache inspection ──
val stats = engine.cacheStats.value
println("${stats.totalEntries} themes, ${stats.hitRate * 100}% hit rate")
println("${stats.entriesBySource[ThemeSource.LLM]} from LLM, ${stats.entriesBySource[ThemeSource.SERVER]} from server")

val entries = engine.cache.entries()        // all entries with metadata
entries.forEach { entry ->
    println("${entry.key}: ${entry.source}, created ${entry.createdAt}")
}

engine.observeCacheEvents().collect { event ->
    when (event) {
        is CacheEvent.Inserted -> log("Cached: ${event.key}")
        is CacheEvent.Evicted -> log("Evicted: ${event.key}")
        is CacheEvent.Cleared -> log("Cache cleared")
        else -> {}
    }
}

// ── Scoped theming in Compose ──
// Just wrap any subtree — the key handles everything
HalogenTheme(key = "coffee") {
    CoffeeScreen()
}
```

### 7.5 Convenience Composable for Settings

```kotlin
// Drop-in settings component
@Composable
fun HalogenSettingsCard(
    modifier: Modifier = Modifier,
    onThemeChanged: ((HalogenThemeSpec) -> Unit)? = null
) {
    // Renders:
    // - Text field for natural language input
    // - "Generate" button
    // - Loading indicator during Nano inference
    // - Preview strip showing primary/secondary/tertiary colors
    // - "Apply" / "Reset" buttons
    // - Error handling UI
}
```

---

## 8. Custom Extensions

Developers can register custom theme tokens that the LLM will also populate.

### Registration

```kotlin
val halogen = Halogen.Builder(context)
    .defaultTheme(HalogenDefaults.light())
    .extensions(
        HalogenExtension("success", "A green-ish color for success states"),
        HalogenExtension("warning", "An amber/yellow color for warnings"),
        HalogenExtension("info", "A blue color for informational states"),
        HalogenExtension("brandGradientStart", "Start color for brand gradient"),
        HalogenExtension("brandGradientEnd", "End color for brand gradient"),
    )
    .store(HalogenStore.dataStore(context))
    .build()
```

### How It Works

When extensions are registered, the prompt automatically appends:

```
Also include these custom color tokens in an "ext" object:
- "success": A green-ish color for success states
- "warning": An amber/yellow color for warnings
- "info": A blue color for informational states
- "brandGradientStart": Start color for brand gradient
- "brandGradientEnd": End color for brand gradient
```

The LLM output then includes:

```json
{
  "pri": "#1A73E8",
  ...
  "ext": {
    "success": "#2E7D32",
    "warning": "#F9A825",
    "info": "#1565C0",
    "brandGradientStart": "#1A73E8",
    "brandGradientEnd": "#6DD5FA"
  }
}
```

### Token Impact

Each extension adds ~15 tokens to input (description) and ~10 tokens to output (key-value). With 5 extensions: +75 input, +50 output. Total output becomes ~115-130 tokens. Still well under 256.

**Recommended maximum: 10 custom extensions** to stay safely within token limits.

### Accessing Extensions

```kotlin
@Composable
fun SuccessBanner(message: String) {
    val successColor = HalogenTheme.extensions["success"]?.toColor()
        ?: Color(0xFF2E7D32)  // fallback
    
    Surface(color = successColor) {
        Text(message, color = Color.White)
    }
}
```

---

## 9. Persistence & Keyed Theme Cache

Themes are **persisted to disk by default** with an **in-memory LRU in front for speed.** The first access for a key reads from disk (Room/SQLite, ~1-5ms) and promotes to memory. Every subsequent access is a HashMap lookup — effectively free. Themes on disk survive process death, app restarts, and device reboots. A theme generated today is still available months later without another LLM call. Themes are stored by **key** — any string the developer defines. A key could be a route (`/r/coffee`), a category (`sports`), a user ID, a brand name, or anything. The first time a key is encountered, the LLM generates a theme and it's written to both memory and disk. Every subsequent time, it loads from memory in nanoseconds.

### 9.1 The ThemeCache Interface

```kotlin
package halogen

interface ThemeCache {

    // ── Core CRUD ──

    /** Get a cached theme by key. Returns null on miss. */
    suspend fun get(key: String): HalogenThemeSpec?

    /** Store a theme for a key. */
    suspend fun put(key: String, spec: HalogenThemeSpec, source: ThemeSource = ThemeSource.LLM)

    /** Check if a key exists without loading the full spec. */
    suspend fun contains(key: String): Boolean

    /** Remove a specific key. */
    suspend fun evict(key: String)

    /** Remove multiple keys. */
    suspend fun evict(keys: Set<String>)

    /** Remove all cached themes. */
    suspend fun clear()

    // ── Inspection ──

    /** All cached keys. */
    suspend fun keys(): Set<String>

    /** Number of cached themes. */
    suspend fun size(): Int

    /** Metadata for a cached theme (without loading the full spec). */
    suspend fun metadata(key: String): ThemeCacheEntry?

    /** All entries with metadata. */
    suspend fun entries(): List<ThemeCacheEntry>

    /** Total storage size in bytes (approximate). */
    suspend fun sizeInBytes(): Long

    // ── Observation ──

    /** Observe cache changes (insertions, evictions, clears). */
    fun observeChanges(): Flow<CacheEvent>
}

data class ThemeCacheEntry(
    val key: String,
    val source: ThemeSource,     // how this theme was generated
    val createdAt: Long,         // epoch millis
    val lastAccessedAt: Long,    // epoch millis
    val sizeBytes: Int           // approximate size of the serialized spec
)

enum class ThemeSource {
    LLM,      // generated by an LLM provider
    SERVER,   // fetched from HalogenServerProvider
    MANUAL    // injected via engine.apply()
}

sealed class CacheEvent {
    data class Inserted(val key: String, val source: ThemeSource) : CacheEvent()
    data class Evicted(val key: String) : CacheEvent()
    data class EvictedBatch(val keys: Set<String>) : CacheEvent()
    data object Cleared : CacheEvent()
}
```

### 9.2 Two-Level Cache (Memory + Disk)

Every cache implementation uses a **memory layer in front of disk.** The first `resolve()` for a key reads from disk and promotes to memory. Every subsequent access is a direct memory lookup — no I/O, no deserialization. This matters when the user is rapidly navigating between themed contexts (swiping between subreddits, tabbing between categories).

```
resolve("coffee")
       │
       ▼
  ┌─ L1: Memory (LinkedHashMap, LRU) ─┐
  │  In-process, nanosecond access     │
  │  HIT? → return HalogenThemeSpec    │
  └────────────────────────────────────┘
       │ MISS
       ▼
  ┌─ L2: Disk (Room / SQLite) ────────┐
  │  Persistent, ~1-5ms access         │
  │  HIT? → promote to L1, return     │
  └────────────────────────────────────┘
       │ MISS
       ▼
  Server → LLM → Default
  (result written to both L1 and L2)
```

### 9.3 Built-in Implementations

```kotlin
// Room-backed with in-memory LRU in front. This is the default.
// L1: 20 most recent themes in memory (nanosecond access)
// L2: unlimited on disk in SQLite (survives process death, restarts, reboots)
val cache = HalogenCache.room(context)

// Room-backed with custom memory and disk limits.
val cache = HalogenCache.room(
    context = context,
    memoryMaxEntries = 30,    // L1: keep 30 in memory
    diskMaxEntries = 200      // L2: keep 200 on disk (LRU eviction)
)

// In-memory only — no disk persistence. Cleared on process death.
// Use for testing or when persistence isn't needed.
val cache = HalogenCache.memory(maxEntries = 50)

// No caching — every resolve() hits the LLM. Development only.
val cache = HalogenCache.none()
```

**The default is two-level.** `HalogenCache.room(context)` gives you an in-memory LRU (L1) backed by SQLite on disk (L2). A theme generated last Tuesday is on disk — the first access loads it into memory in ~1-5ms, every access after that is a HashMap lookup. When the user toggles between Coffee and Ocean repeatedly, neither hits disk after the first load.

### 9.4 Cache Behavior Details

| Operation | L1 (Memory) | L2 (Disk) |
|-----------|-------------|-----------|
| `resolve()` hit | Read from L1, no disk I/O | Not touched |
| `resolve()` L1 miss, L2 hit | Promoted to L1 | Read from disk |
| `resolve()` full miss → LLM | Written to L1 | Written to L2 |
| `evict(key)` | Removed from L1 | Removed from L2 |
| `clearCache()` | L1 cleared | L2 cleared |
| `put(key, spec)` | Written to L1 | Written to L2 |
| Process death | L1 lost | L2 survives |
| App restart | L1 empty (cold start) | L2 still has everything |
| LRU eviction (L1 full) | Oldest L1 entry dropped | L2 still has it — will be re-promoted on next access |
| LRU eviction (L2 full) | Not affected | Oldest L2 entry deleted from disk |

### 9.5 Room Entity (Internal)

```kotlin
@Entity(tableName = "halogen_themes")
internal data class ThemeEntity(
    @PrimaryKey val key: String,
    val spec: String,           // JSON-serialized HalogenThemeSpec
    val createdAt: Long,        // epoch millis
    val lastAccessedAt: Long,   // for LRU eviction
    val source: String,         // "llm", "server", "manual"
    val sizeBytes: Int          // byte length of spec JSON
)
```

### 9.6 The Resolve Flow — Cache → Server → LLM → Default

This is the key API. When the app navigates to a new context (subreddit, category, screen), it calls `resolve()` with a key and an optional hint. The engine checks sources in priority order:

```kotlin
// The main API for contextual theming
val spec = engine.resolve(
    key = "/r/coffee",
    hint = "coffee, warm browns, cozy café"  // natural language hint for LLM
)
```

Under the hood:

```
engine.resolve(key, hint)
         │
         ▼
    ┌─ 1. L1 Memory cache ─────────────────────┐
    │   memoryCache.get("coffee")                │
    │   HIT? → return immediately (nanoseconds)  │
    └────────────────────────────────────────────┘
         │ MISS
         ▼
    ┌─ 2. L2 Disk cache (Room/SQLite) ──────────┐
    │   diskCache.get("coffee")                  │
    │   HIT? → promote to L1, return (~1-5ms)    │
    └────────────────────────────────────────────┘
         │ MISS
         ▼
    ┌─ 3. Server provider (optional) ───────────┐
    │   serverProvider?.fetchTheme("coffee")      │
    │   HIT? → write L1 + L2, return             │
    └────────────────────────────────────────────┘
         │ MISS or no server provider
         ▼
    ┌─ 4. LLM provider chain ──────────────────┐
    │   providers[0].availability() → READY?     │
    │     YES → generate(prompt) → parse/validate│
    │     NO  → try providers[1] → ...           │
    │   SUCCESS? → write L1 + L2, return         │
    └────────────────────────────────────────────┘
         │ ALL FAIL
         ▼
    ┌─ 5. Fallback ─────────────────────────────┐
    │   Return defaultTheme                      │
    └────────────────────────────────────────────┘
```

### 9.7 Engine API for Keyed Themes

```kotlin
class HalogenEngine {

    // ── Active Theme ──

    /** Active theme StateFlow — drives HalogenTheme recomposition */
    val activeTheme: StateFlow<HalogenThemeSpec?>

    /** The key of the currently active theme (null = default) */
    val activeKey: StateFlow<String?>

    // ── Resolve ──

    /**
     * Resolve a theme for a key. Checks cache first (if enabled), then server,
     * then LLM. Caches the result (if caching enabled). Applies it as active.
     *
     * @param key   Unique identifier (route, category, ID, etc.)
     * @param hint  Natural language description for LLM generation.
     *              Ignored if theme is found in cache or server.
     * @return The resolved theme result
     */
    suspend fun resolve(
        key: String,
        hint: String? = null
    ): HalogenResult

    /**
     * Resolve without applying — useful for prefetching or previewing.
     */
    suspend fun prefetch(key: String, hint: String? = null): HalogenResult

    /**
     * Apply a previously resolved or server-provided theme for a key.
     * Also caches it.
     */
    suspend fun apply(key: String, spec: HalogenThemeSpec)

    /**
     * Apply the default theme (clears active key).
     */
    fun applyDefault()

    // ── Cache Management ──

    /**
     * Access the underlying cache for direct inspection.
     */
    val cache: ThemeCache

    /**
     * Enable or disable caching at runtime.
     * When disabled, resolve() always hits server/LLM — useful for
     * development, testing, or a "always fresh" mode.
     * When re-enabled, previously cached themes are still available.
     */
    var cachingEnabled: Boolean

    /**
     * Refresh a single key: evict it from cache and re-resolve.
     * Uses the original hint if available, or derives one from the key.
     */
    suspend fun refresh(key: String, hint: String? = null): HalogenResult

    /**
     * Refresh all cached themes. Iterates every cached key,
     * re-generates each via LLM, and overwrites the cache.
     * Returns a map of key → result.
     * 
     * Use with caution — this can trigger many LLM calls.
     * Consider the provider's quota/rate limits.
     *
     * @param concurrency  Max parallel LLM calls (default 1, sequential)
     */
    suspend fun refreshAll(concurrency: Int = 1): Map<String, HalogenResult>

    /**
     * Generate a new theme for a key, bypassing cache.
     * Overwrites any existing cached theme for this key.
     * Unlike refresh(), this requires an explicit hint.
     */
    suspend fun regenerate(key: String, hint: String): HalogenResult

    /**
     * Evict a single cached theme.
     */
    suspend fun evict(key: String)

    /**
     * Evict multiple cached themes.
     */
    suspend fun evict(keys: Set<String>)

    /**
     * Evict all cached themes.
     */
    suspend fun clearCache()

    /**
     * Evict themes older than a duration.
     * Useful for periodic cleanup.
     */
    suspend fun evictOlderThan(duration: Duration)

    /**
     * Evict themes from a specific source.
     * e.g., clear all LLM-generated themes but keep server-provided ones.
     */
    suspend fun evictBySource(source: ThemeSource)

    // ── Cache Observation ──

    /**
     * Cache stats as a StateFlow for UI display.
     */
    val cacheStats: StateFlow<CacheStats>

    /**
     * Observe cache events (insertions, evictions, clears).
     */
    fun observeCacheEvents(): Flow<CacheEvent>
}

data class CacheStats(
    val totalEntries: Int,
    val totalSizeBytes: Long,
    val hitCount: Long,          // total cache hits since engine creation
    val missCount: Long,         // total cache misses since engine creation
    val hitRate: Float,          // hitCount / (hitCount + missCount)
    val oldestEntryAge: Duration?,
    val newestEntryAge: Duration?,
    val entriesBySource: Map<ThemeSource, Int>
)
```

### 9.8 Server Provider (Optional)

For apps like Reddit where the server might define themes for known entities:

```kotlin
// Developer implements this interface
fun interface HalogenServerProvider {
    suspend fun fetchTheme(key: String): HalogenThemeSpec?
}

// Example: fetch subreddit theme from your API
class RedditThemeProvider(private val api: RedditApi) : HalogenServerProvider {
    override suspend fun fetchTheme(key: String): HalogenThemeSpec? {
        return try {
            api.getSubredditTheme(key)  // returns HalogenThemeSpec or null
        } catch (e: Exception) {
            null  // fall through to LLM
        }
    }
}

// Wire it up
val halogen = Halogen.Builder(context)
    .defaultTheme(HalogenDefaults.light())
    .cache(HalogenCache.room(context, maxEntries = 200))
    .serverProvider(RedditThemeProvider(api))
    .build()
```

### 9.9 Hint Generation from Context

Sometimes the developer doesn't have a natural language hint — they just have a key. The library can auto-generate a hint from the key itself:

```kotlin
// The key IS the hint
engine.resolve(key = "coffee", hint = "coffee")

// Or let the library derive it
engine.resolve(key = "/r/coffee")
// Internally: strips route prefixes, splits camelCase/snake_case
// → sends "coffee" as the hint to Nano
```

Auto-hint extraction rules (internal):
- Strip common prefixes: `/r/`, `/category/`, `/topic/`, `#`
- Split camelCase: `darkForest` → `dark forest`
- Split snake_case: `dark_forest` → `dark forest`
- Split kebab-case: `dark-forest` → `dark forest`
- If result is empty or just an ID, skip LLM → use default

### 9.10 Composable Integration — Scoped Theming

```kotlin
// Automatically resolves and applies theme for a key
@Composable
fun HalogenTheme(
    key: String,
    hint: String? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    fallback: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    val engine = LocalHalogenEngine.current
    var resolved by remember { mutableStateOf(false) }

    LaunchedEffect(key) {
        engine.resolve(key, hint)
        resolved = true
    }

    val themeSpec by engine.activeTheme.collectAsState()

    if (!resolved && themeSpec == null) {
        fallback()
    }
    
    // Expands and applies via MaterialTheme (same as keyless version)
    HalogenThemeInternal(themeSpec, darkTheme, content)
}

// Usage in navigation
@Composable
fun SubredditScreen(subredditName: String) {
    HalogenTheme(
        key = subredditName,
        hint = subredditName,  // "coffee" → generates coffee theme
        fallback = { CircularProgressIndicator() }
    ) {
        // Everything inside gets the subreddit's theme
        SubredditContent(subredditName)
    }
}
```

### 9.11 Prefetching

Preload themes before the user navigates there:

```kotlin
// In a ViewModel or when loading a list of subreddits
viewModelScope.launch {
    subreddits.forEach { sub ->
        engine.prefetch(key = sub.name, hint = sub.name)
    }
}
```

### 9.12 Theme Export & Import

Themes are just JSON. Sharing is trivial:

```kotlin
// Export
val json = engine.cache.get("coffee")?.toJson()
// → copy to clipboard, save to file, send to server

// Import
val spec = HalogenThemeSpec.fromJson(jsonString)
engine.apply("my-custom-theme", spec)
```

### 9.13 Debouncing & Concurrency

```kotlin
// Internal to HalogenEngine
private val resolveJobs = ConcurrentHashMap<String, Job>()

suspend fun resolve(key: String, hint: String?): HalogenResult {
    // Cancel any in-flight resolve for this same key
    resolveJobs[key]?.cancel()
    
    return coroutineScope {
        val job = launch {
            // ... cache → server → LLM → fallback
        }
        resolveJobs[key] = job
        job.join()
        // ...
    }
}
```

### 9.14 Cache Warming on App Start

```kotlin
// In Application.onCreate()
val halogen = Halogen.Builder(context)
    .defaultTheme(HalogenDefaults.light())
    .cache(HalogenCache.room(context))
    .warmup(listOf("coffee", "sports", "music", "tech"))  // prefetch on init
    .build()
```

---

## 10. Accessibility & Safety

### Contrast Validation

After the LLM generates colors, before applying:

```kotlin
fun validateContrast(scheme: HalogenColorScheme): ValidationResult {
    val issues = mutableListOf<ContrastIssue>()
    
    // WCAG AA requires 4.5:1 for normal text, 3:1 for large text
    checkContrast(scheme.primary, scheme.onPrimary, "primary/onPrimary", issues)
    checkContrast(scheme.secondary, scheme.onSecondary, "secondary/onSecondary", issues)
    checkContrast(scheme.surface, scheme.onSurface, "surface/onSurface", issues)
    checkContrast(scheme.background, scheme.onBackground, "background/onBackground", issues)
    checkContrast(scheme.error, scheme.onError, "error/onError", issues)
    // ... all on* / container pairs
    
    return if (issues.isEmpty()) ValidationResult.Pass
           else ValidationResult.Fail(issues)
}
```

If validation fails, the library **auto-corrects** by adjusting tone levels to meet contrast requirements, before applying the theme. The user gets their desired palette, but accessible.

### Safety Guardrails

The LLM prompt includes:
- No offensive content can leak through colors (colors are just hex values, inherently safe)
- Typography stays within system fonts (no external font loading attack surface)
- Shape values are clamped to valid ranges

The library validates all output:
- Hex colors must match `#[0-9A-Fa-f]{6}` regex
- Font weights clamped to 100-900
- Corner scale clamped to 0.0-2.0
- Unknown `fontMood` or `cornerStyle` values fall back to defaults
- Malformed JSON triggers retry with temperature=0, then falls back to default theme

---

## 11. Graceful Degradation

| Scenario                        | Behavior                                |
|--------------------------------|------------------------------------------|
| Primary provider available      | Generate with primary provider            |
| Primary unavailable, fallback set | Auto-failover to fallback provider      |
| All providers unavailable       | Use dev's default theme; `HalogenSettingsCard` shows "unavailable" state |
| Provider returns invalid JSON   | Retry once with same provider. If still fails, try next provider. If all fail, use default |
| Contrast validation failure     | Auto-correct tones, apply corrected theme |
| App in background               | Queue request, execute when foregrounded (Nano constraint only) |
| No provider configured          | Themes only come from cache or server provider; no generation |

### Availability Check API

```kotlin
@Composable
fun SettingsScreen() {
    val engine = LocalHalogenEngine.current
    val availability by engine.availability.collectAsState()
    
    when (availability) {
        HalogenAvailability.Ready -> HalogenSettingsCard()
        HalogenAvailability.Downloading -> DownloadProgressCard()
        HalogenAvailability.Unavailable -> { /* don't show AI theming */ }
    }
}
```

---

## 12. Module Structure

```
halogen/
│
├── README.md
├── DESIGN.md                              # Architectural decisions (see Section 16)
├── LICENSE
│
├── halogen-core/                           # Pure Kotlin, no Android deps
│   └── src/main/kotlin/halogen/
│       ├── HalogenThemeSpec.kt             # Schema data class + serialization
│       ├── HalogenLlmProvider.kt           # LLM provider interface
│       ├── HalogenColorScheme.kt           # 49-color expanded scheme
│       ├── HalogenTypography.kt            # 15 text styles
│       ├── HalogenShapes.kt                # 5 shape sizes
│       ├── HalogenExtensions.kt            # Custom token support
│       ├── HalogenDefaults.kt              # Built-in default themes
│       ├── ThemeExpander.kt                # Seed → full palette expansion
│       ├── ContrastValidator.kt            # WCAG contrast checking
│       ├── SchemaParser.kt                 # JSON → HalogenThemeSpec
│       ├── PromptBuilder.kt               # System prompt + few-shot construction
│       └── color/                          # Color science
│           ├── Hct.kt                      # HCT color space
│           ├── TonalPalette.kt             # 13-tone palette generation
│           └── DynamicScheme.kt            # Seed → M3 role mapping
│
├── halogen-compose/                        # Compose UI layer
│   └── src/main/kotlin/halogen/compose/
│       ├── HalogenTheme.kt                 # CompositionLocalProvider wrapper
│       ├── HalogenSettingsCard.kt          # Drop-in settings UI
│       ├── HalogenPreview.kt               # Theme preview composable
│       └── Locals.kt                       # CompositionLocals
│
├── halogen-engine/                         # Orchestration + persistence (no LLM dep)
│   └── src/main/kotlin/halogen/engine/
│       ├── HalogenEngine.kt                # resolve, prefetch, cache orchestration
│       ├── Halogen.kt                      # Builder + factory
│       ├── HalogenResult.kt               # Sealed result type
│       ├── ThemeCache.kt                   # Cache interface
│       ├── TwoLevelThemeCache.kt          # L1 memory + L2 disk orchestrator
│       ├── MemoryThemeCache.kt            # In-memory LRU (L1, or standalone)
│       ├── RoomThemeCache.kt              # Room-backed disk cache (L2)
│       ├── HalogenCache.kt               # Cache factory (room, memory, none)
│       ├── HalogenServerProvider.kt       # Optional server fetch interface
│       └── db/
│           ├── ThemeEntity.kt             # Room entity
│           └── ThemeDao.kt                # Room DAO
│
├── halogen-provider-nano/                  # Gemini Nano provider (separate artifact)
│   └── src/main/kotlin/halogen/provider/nano/
│       └── GeminiNanoProvider.kt           # ML Kit Prompt API implementation
│
└── sample-app/                             # Habitat demo application
    └── src/main/kotlin/me/mmckenna/halogen/sample/
        ├── MainActivity.kt
        ├── HabitatApp.kt
        ├── PlaygroundScreen.kt
        ├── HomeScreen.kt
        ├── CommunityScreen.kt
        ├── ThemeInspectorSheet.kt
        └── SettingsScreen.kt
```

### Imports (for library consumers)

```kotlin
// Core types — always available
import halogen.HalogenThemeSpec
import halogen.HalogenLlmProvider
import halogen.HalogenDefaults

// Compose UI
import halogen.compose.HalogenTheme
import halogen.compose.HalogenSettingsCard

// Engine
import halogen.engine.HalogenEngine
import halogen.engine.Halogen
import halogen.engine.HalogenCache

// Gemini Nano provider (only if using this artifact)
import halogen.provider.nano.GeminiNanoProvider
```

### Maven Coordinates

```
Group ID:    me.mmckenna.halogen
Artifact IDs:
  halogen-core              → halogen.* (interfaces, schema, color science)
  halogen-compose           → halogen.compose.* (Composables)
  halogen-engine            → halogen.engine.* (orchestration, cache, Room)
  halogen-provider-nano     → halogen.provider.nano.* (Gemini Nano)
```

### Gradle Dependencies (for library consumers)

```kotlin
// ── Minimal: just the theming infra, BYO provider ──
implementation("me.mmckenna.halogen:halogen-core:0.1.0")
implementation("me.mmckenna.halogen:halogen-compose:0.1.0")
implementation("me.mmckenna.halogen:halogen-engine:0.1.0")

// ── Add Gemini Nano (on-device, no API key needed) ──
implementation("me.mmckenna.halogen:halogen-provider-nano:0.1.0")

// ── Or write your own provider ──
// Just implement halogen.HalogenLlmProvider in your app code
// No extra dependency needed — the interface is in halogen-core

// Transitive deps:
// halogen-core        → kotlinx-serialization-json, material-color-utilities
// halogen-compose     → halogen-core, androidx.compose.material3
// halogen-engine      → halogen-core, androidx.room
// halogen-provider-nano → halogen-core, com.google.mlkit:genai-prompt
```

### Docs Site

```
https://halogen.mmckenna.me
```

---

## 13. Kotlin Multiplatform

### 13.1 Platform Support Matrix

| Platform | HalogenTheme | Keyed Cache | Gemini Nano | Cloud LLM Providers |
|----------|:---:|:---:|:---:|:---:|
| **Android** | ✅ | ✅ Room | ✅ `halogen-provider-nano` | ✅ |
| **iOS** | ✅ CMP | ✅ Room KMP | ❌ | ✅ |
| **Desktop (JVM)** | ✅ CMP | ✅ Room KMP | ❌ | ✅ |
| **Web (Wasm)** | ✅ CMP | ⚠️ In-memory only | ❌ | ✅ |

Gemini Nano is Android-only and always will be (it's an AICore system service). But because the provider is a separate artifact, every other platform works perfectly with cloud providers. The theming engine, cache, color science, and Compose integration are all platform-agnostic.

### 13.2 Module → KMP Source Set Mapping

```
halogen-core/                              # kotlin-multiplatform plugin
├── src/commonMain/kotlin/halogen/         # ALL core code lives here
│   ├── HalogenThemeSpec.kt                # @Serializable, pure Kotlin
│   ├── HalogenLlmProvider.kt             # interface, pure Kotlin
│   ├── HalogenExtensions.kt              # pure Kotlin
│   ├── HalogenDefaults.kt                # pure Kotlin
│   ├── ThemeExpander.kt                   # pure Kotlin
│   ├── ContrastValidator.kt               # pure Kotlin (WCAG math)
│   ├── SchemaParser.kt                    # kotlinx.serialization (KMP)
│   ├── PromptBuilder.kt                   # string building, pure Kotlin
│   └── color/                             # pure Kotlin math (NO Java deps)
│       ├── Hct.kt                         # HCT ↔ sRGB, ~200 lines of math
│       ├── TonalPalette.kt               # 13-tone generation, ~100 lines
│       └── DynamicScheme.kt              # seed → M3 role mapping, ~150 lines
├── src/commonTest/                        # shared tests
└── build.gradle.kts                       # targets: jvm, iosArm64, iosSimulatorArm64, wasmJs

halogen-compose/                           # kotlin-multiplatform + compose plugin
├── src/commonMain/kotlin/halogen/compose/
│   ├── HalogenTheme.kt                   # CMP MaterialTheme wrapping
│   ├── HalogenSettingsCard.kt            # CMP Composables
│   ├── HalogenPreview.kt                 # CMP Composables
│   └── Locals.kt                          # CompositionLocals
└── build.gradle.kts                       # targets: all CMP targets

halogen-engine/                            # kotlin-multiplatform
├── src/commonMain/kotlin/halogen/engine/
│   ├── HalogenEngine.kt                  # resolve chain, pure Kotlin
│   ├── Halogen.kt                         # Builder
│   ├── HalogenResult.kt                  # sealed class
│   ├── ThemeCache.kt                      # interface
│   ├── MemoryThemeCache.kt               # in-memory LRU, pure Kotlin
│   └── HalogenServerProvider.kt          # interface
├── src/commonMain/kotlin/halogen/engine/db/
│   ├── ThemeEntity.kt                     # Room entity (Room KMP)
│   └── ThemeDao.kt                        # Room DAO (Room KMP)
├── src/androidMain/kotlin/halogen/engine/
│   └── RoomThemeCache.android.kt          # Room Android impl
├── src/iosMain/kotlin/halogen/engine/
│   └── RoomThemeCache.ios.kt              # Room iOS impl
├── src/jvmMain/kotlin/halogen/engine/
│   └── RoomThemeCache.jvm.kt              # Room JVM impl
├── src/wasmJsMain/kotlin/halogen/engine/
│   └── LocalStorageThemeCache.kt          # Web: localStorage-backed cache
└── build.gradle.kts

halogen-provider-nano/                     # ANDROID ONLY (not KMP)
├── src/main/kotlin/halogen/provider/nano/
│   └── GeminiNanoProvider.kt
└── build.gradle.kts                       # android library plugin only
```

### 13.3 The Color Science Decision

> **Decision:** Halogen includes a pure Kotlin implementation of Material 3 color science (HCT color space, tonal palette generation, dynamic scheme mapping) rather than depending on Google's `material-color-utilities` Java library.

#### The Problem

Material 3's theming system relies on the HCT (Hue-Chroma-Tone) color space to expand a small set of seed colors into a full harmonious palette of 49 color roles. Google publishes this logic as [`material-color-utilities`](https://github.com/material-foundation/material-color-utilities) — an open-source library with implementations in Java, TypeScript, Dart, and C++.

The Java implementation works on Android. But Halogen targets Compose Multiplatform — Android, iOS, Desktop, and Web. The Java library cannot run on iOS (no JVM), cannot run in the browser (Kotlin/Wasm), and creates an unnecessary platform dependency on Desktop even though a JVM is available there.

#### Options Considered

| Option | Pros | Cons |
|--------|------|------|
| **A. Depend on `material-color-utilities` (Java)** | Zero effort, battle-tested | Android-only. Kills KMP. iOS, Web, Desktop all blocked. |
| **B. `expect/actual` wrapper** | Java on Android, ??? on other platforms | Still need to solve iOS/Web. Two implementations to maintain. Doesn't actually reduce work. |
| **C. Wrap the TypeScript version for Wasm** | Reuses existing code | Fragile interop. Different implementations per platform. Testing nightmare. |
| **D. Pure Kotlin implementation in `commonMain`** | Runs everywhere. Single implementation. Fully testable. No platform deps. | ~450 lines of math to write and validate. |

#### Decision: Option D — Pure Kotlin

The math is well-specified, published, and small:

1. **HCT ↔ sRGB conversion** (~200 lines) — CAM16 color appearance model with matrix transforms and Newton-Raphson root finding. The algorithm is documented in the [Material Design 3 spec](https://m3.material.io/styles/color/system/how-the-system-works) and the [Java source](https://github.com/material-foundation/material-color-utilities/tree/main/java/com/google/material/color) is Apache 2.0 licensed.

2. **TonalPalette generation** (~100 lines) — given a color in HCT, produce 13 tonal variants by holding hue and chroma constant and varying tone across the levels [0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 95, 99, 100]. Straightforward once HCT conversion works.

3. **DynamicScheme mapping** (~150 lines) — assign the 13 tones from each of the 5 palettes (primary, secondary, tertiary, neutral, error) to the 49 M3 `ColorScheme` roles. This is a lookup table, not computation. The mapping differs between light and dark modes and is documented in the M3 spec.

**Total: ~450 lines of pure Kotlin.** No platform dependencies. All testable against reference outputs from Google's Java library. Every test vector from the Java test suite can be ported.

#### Why This Is Worth It

- **Enables KMP with zero `expect/actual` for color.** The same code runs on every platform identically.
- **No transitive Java dependency.** Consumers on iOS/Web don't pull a JVM library they can't use.
- **Fully testable in `commonTest`.** Color math has deterministic outputs — we validate against Google's reference implementation with exact hex-value assertions.
- **Maintenance is minimal.** HCT and the M3 tone-mapping spec are stable. Google hasn't changed the core color math since M3 launched. If they do, the Java source is open and we update accordingly.
- **The alternative is worse.** Options B and C both require maintaining multiple platform-specific implementations of the same math, which is strictly more work than one shared implementation.

#### Validation Strategy

- Port the [Java test suite](https://github.com/material-foundation/material-color-utilities/tree/main/java/com/google/material/color) for HCT round-trip accuracy, tonal palette generation, and scheme mapping.
- Fuzz test: generate 10,000 random seed colors, expand via both our Kotlin impl and Google's Java impl (on JVM), assert identical hex output for all 49 roles.
- Visual test: render the same seed color through both implementations side-by-side in the sample app.

### 13.4 Platform-Specific Provider Examples

```kotlin
// ── Android: on-device Gemini Nano ──
// Just add halogen-provider-nano artifact
val provider = GeminiNanoProvider(context)

// ── iOS: cloud provider in Swift interop ──
// Developer implements HalogenLlmProvider in their KMP shared module
class GeminiCloudProvider(private val apiKey: String) : HalogenLlmProvider {
    override suspend fun generate(prompt: String): String {
        // Ktor HttpClient call to Gemini API (KMP-compatible)
        return ktorClient.post("https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent") {
            // ...
        }.bodyAsText()
    }
    override suspend fun availability() = HalogenLlmAvailability.READY
}

// ── Desktop: local Ollama instance ──
class OllamaProvider(private val baseUrl: String = "http://localhost:11434") : HalogenLlmProvider {
    override suspend fun generate(prompt: String): String {
        return ktorClient.post("$baseUrl/api/generate") {
            setBody("""{"model":"llama3","prompt":"$prompt","stream":false}""")
        }.bodyAsText()
    }
    override suspend fun availability() = HalogenLlmAvailability.READY
}

// ── Web: browser-based API call ──
class WebLlmProvider(private val apiKey: String) : HalogenLlmProvider {
    override suspend fun generate(prompt: String): String {
        // Uses Ktor JS engine or kotlinx-browser fetch API
        return window.fetch("https://api.openai.com/v1/chat/completions", /* ... */)
            .await().text().await()
    }
    override suspend fun availability() = HalogenLlmAvailability.READY
}
```

### 13.5 Cache Strategy Per Platform

| Platform | Primary Cache | Persistent? | Fallback | Notes |
|----------|--------------|:-----------:|----------|-------|
| Android | Room (SQLite) | ✅ Disk | Memory LRU | Survives process death, restarts, reboots |
| iOS | Room KMP (SQLite) | ✅ Disk | Memory LRU | Same as Android |
| Desktop | Room KMP (SQLite) | ✅ Disk | Memory LRU | Same as Android |
| Web (Wasm) | `localStorage` | ✅ Browser storage | Memory LRU | ~5MB browser limit, but theme JSON is ~200 bytes each. Survives tab close and browser restart. |

### 13.6 What Stays Android-Only

| Component | Why | Impact |
|-----------|-----|--------|
| `halogen-provider-nano` | ML Kit + AICore are Android system services | iOS/Desktop/Web use cloud providers instead |
| `GeminiNanoProvider.ensureDownloaded()` | Model download is Android-specific | Other platforms don't need on-device models |
| Foreground-only constraint | AICore limitation | Only affects Nano provider |

### 13.7 Build Configuration

```kotlin
// halogen-core/build.gradle.kts
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    androidTarget()
    iosArm64()
    iosSimulatorArm64()
    jvm("desktop")
    wasmJs { browser() }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

// halogen-provider-nano/build.gradle.kts
plugins {
    id("com.android.library")  // NOT multiplatform
    kotlin("android")
}

dependencies {
    implementation(project(":halogen-core"))
    implementation(libs.mlkit.genai.prompt)
}
```

### 13.8 Migration Path

The plan accounts for this by building KMP-ready from day one:

1. **Phase 1-2**: Build `halogen-core` and `halogen-compose` as KMP modules. All code in `commonMain`. Color science in pure Kotlin from the start.
2. **Phase 3**: Build `halogen-engine` as KMP with `expect/actual` for Room (Android/iOS/Desktop) and localStorage (Web). `halogen-provider-nano` as Android-only.
3. **Phase 4**: Sample app is Android-first (Habitat), but the library itself works everywhere.
4. **Post-launch**: Add an iOS sample app (SwiftUI host with CMP theming), Desktop demo, and Web demo.

The provider abstraction was the architectural decision that made this possible. Without it, the whole library would be welded to ML Kit.

---

## 14. Build Phases

### Phase 1: Core Foundation (Week 1-2)

- [ ] Set up KMP project structure (`kotlin("multiplatform")` for core, compose, engine)
- [ ] Pure Kotlin HCT color space implementation in `commonMain` (~200 lines)
- [ ] Pure Kotlin TonalPalette generation in `commonMain` (~100 lines)
- [ ] Pure Kotlin DynamicScheme (seed → 49 M3 roles) in `commonMain` (~150 lines)
- [ ] `HalogenThemeSpec` data class with kotlinx.serialization in `commonMain`
- [ ] `HalogenLlmProvider` interface in `commonMain`
- [ ] `SchemaParser` — JSON parsing with validation and fallback
- [ ] `ThemeExpander` — seed colors → full 49-color palette using tonal palettes
- [ ] `HalogenColorScheme` → `M3 ColorScheme` conversion
- [ ] `HalogenTypography` → `M3 Typography` conversion
- [ ] `HalogenShapes` → `M3 Shapes` conversion
- [ ] `ContrastValidator` — WCAG AA contrast checking + auto-correction
- [ ] `commonTest` unit tests for color math, expansion, parsing, validation

### Phase 2: Compose Integration (Week 2-3)

- [ ] `HalogenTheme` composable with `CompositionLocalProvider` (CMP `commonMain`)
- [ ] `LocalHalogenExtensions` for custom tokens
- [ ] `HalogenDefaults` — light, dark, materialYou presets
- [ ] Verify all CMP M3 components pick up theme correctly (Button, Card, TextField, FAB, NavBar, TopAppBar, BottomSheet, Dialog, Switch, Checkbox, Radio, Slider, Chip, Badge, Snackbar)
- [ ] `HalogenPreview` — small composable showing color swatches

### Phase 3: Engine, Cache & Provider Abstraction (Week 3-4)

- [ ] `ThemeCache` interface + `HalogenCache` factory in `commonMain`
- [ ] `MemoryThemeCache` — in-memory LRU in `commonMain` (works on all platforms)
- [ ] `RoomThemeCache` — Room KMP with `expect/actual` for Android/iOS/Desktop
- [ ] `LocalStorageThemeCache` — `wasmJsMain` for Web
- [ ] `HalogenEngine` — resolve flow: cache → server → provider chain → fallback
- [ ] `Halogen.Builder` — wires providers, cache, server, defaults
- [ ] Provider chaining with automatic failover on `UNAVAILABLE`
- [ ] `HalogenServerProvider` interface (optional server fetch)
- [ ] `HalogenResult` sealed class
- [ ] `GeminiNanoProvider` — ML Kit Prompt API implementation (**Android-only** module)
- [ ] Auto-hint extraction from keys (strip prefixes, split casing)
- [ ] Prefetch API (`engine.prefetch()`)
- [ ] Debouncing, retry logic, concurrent resolve cancellation
- [ ] Theme export/import (JSON serialization)
- [ ] Integration tests: cache hit, miss → provider, server provider, eviction, failover

### Phase 4: Habitat Sample App (Week 4-5)

- [ ] `HalogenSettingsCard` — drop-in natural language input card
- [ ] `HalogenTheme(key)` overload — scoped keyed theming composable
- [ ] Theme preview strip (animated color swatch transition)
- [ ] Habitat Android app: Playground screen (text field + component grid, the hero screen)
- [ ] Habitat Android app: Home Feed screen with community cards (presets + user-created)
- [ ] Habitat Android app: Community Screen with `HalogenTheme(key = communityName)`
- [ ] Habitat Android app: Theme Inspector bottom sheet (color grid, typography, shapes, cache stats)
- [ ] Habitat Android app: Settings screen (global theme, cache management, provider status)
- [ ] "Save to communities" flow from Playground → Home Feed
- [ ] Prefetching on Home Feed load
- [ ] Animated theme transition when navigating between communities

### Phase 5: Polish + Open Source (Week 5-6)

- [ ] Custom extensions documentation + testing
- [ ] API surface review (minimize public API, mark experimental)
- [ ] KDoc on all public types
- [ ] README with quick start, screenshots, architecture diagram
- [ ] `DESIGN.md` — architectural decisions document (color science, provider abstraction, seed expansion, keyed cache, MaterialTheme wrapping)
- [ ] License (Apache 2.0)
- [ ] Maven Central publication setup (KMP artifacts for all targets)
- [ ] GitHub repo + CI (GitHub Actions with multiplatform builds)
- [ ] Demo video / GIF for README

### Phase 6: Multiplatform Verification (Week 6-7, post-launch stretch)

- [ ] iOS sample app (SwiftUI host, CMP theme screen)
- [ ] Desktop demo (Compose Desktop window with Halogen theming)
- [ ] Web demo (Wasm, hosted at halogen.mmckenna.me/demo)
- [ ] Verify Room KMP cache on iOS and Desktop
- [ ] Verify localStorage cache on Web
- [ ] Document platform-specific provider setup for iOS/Desktop/Web
- [ ] Community provider examples (OpenAI, Claude, Ollama)

---

## 15. Sample App — "Habitat"

The demo app is a Reddit-style content browser called **Habitat** ("habitats" as themed spaces, also a chemistry nod — halogens exist in different habitats). Each "habitat" is a themed community that demonstrates contextual theming.

### Concept

The app opens to a **Playground** — a blank screen with a text field. Type anything ("rainy tokyo night", "grandmother's kitchen", "90s skateboard magazine") and watch the entire screen transform with a generated theme. Save your creation as a community, or browse the preset communities — "Coffee", "Ocean", "Cyberpunk", etc. — each with its own cached, contextual theme. Every Material 3 component recolors when you navigate between them.

### Screens

**1. Playground** (the first thing you see)
- Full-screen blank canvas with a single text field at the top: "Describe a vibe..."
- Type anything — "rainy tokyo night", "grandmother's kitchen", "mars colony", "90s skateboard magazine"
- Hit enter or tap Generate
- Loading shimmer across the screen
- Theme materializes: background color shifts, text recolors, a grid of M3 components below the text field comes alive with the new palette
- Components shown: Button, FAB, Card, TextField, Switch, Slider, Chip, NavigationBar, TopAppBar — all themed
- Light/dark toggle in the corner — flips instantly
- "Save to communities" button — saves the prompt as a new community card on the Home Feed
- History of recent generations as small swatch chips below the text field — tap to re-apply
- This screen alone is the entire README GIF

**2. Home Feed**
- List of communities as cards: "Coffee", "Ocean", "Cyberpunk", "Forest", "Sunset", "Minimal", "Retro Gaming", "Jazz", "Arctic", "Lavender"
- Plus any user-created communities from the Playground
- Each card shows a small color swatch strip (prefetched from cache)
- Cards that haven't been visited yet show a subtle "✨ New theme" indicator
- Pull-to-refresh prefetches all visible community themes

**3. Community Screen**
- Navigating here calls `engine.resolve(key = communityName)`
- `HalogenTheme(key = communityName)` wraps the entire screen
- Contains mock posts with titles, body text, upvote buttons, comment counts
- Top app bar, FAB, chips, cards, dividers — all rethemed contextually
- Bottom sheet with community info
- Demonstrates that EVERY M3 component picks up the theme automatically

**4. Theme Inspector** (pull up from any screen)
- Full 49-color grid for **both light and dark** — shown side by side or as a toggle
- Typography scale preview
- Shape preview
- Light/dark mode comparison toggle (instant, no LLM call)
- "Regenerate" button (calls `engine.regenerate()`, bypasses cache)
- "Export JSON" button
- Cache stats (hit/miss count, total cached themes, cache size)

**5. Settings**
- `HalogenSettingsCard` for the global/default theme
- Provider status (Nano available/downloading/unavailable, fallback provider status)
- Toggle: auto-generate themes for new communities vs. use default
- **Cache Management panel:**
  - Live stats display: total entries, hit rate %, total size
  - Entries by source breakdown (LLM vs server vs manual)
  - Toggle: enable/disable caching (`engine.cachingEnabled`)
  - "Clear All" button (`engine.clearCache()`)
  - "Refresh All" button (`engine.refreshAll()`) with progress indicator
  - "Clear Stale" button (`engine.evictOlderThan(30.days)`)
  - Scrollable list of all cached keys with metadata (source, age, size)
  - Swipe-to-delete on individual cache entries
  - "Refresh" action on individual entries

### Navigation Flow

```
Bottom Nav: [Playground] [Communities] [Settings]
                │              │            │
                ▼              │            │
         Playground            │            │
         ├─ type "rainy tokyo" │            │
         │  → theme generates  │            │
         │  → components recolor            │
         ├─ "Save to communities"           │
         │  → appears in Communities tab     │
         │                     │            │
         │                     ▼            │
         │              Home Feed           │
         │                │                 │
         │                ├─ tap "Coffee" ──▶ HalogenTheme(key="coffee") 
         │                │                    └─ warm browns, cream
         │                │                 │
         │                ├─ tap "Ocean"  ──▶ HalogenTheme(key="ocean")
         │                │                    └─ deep blues, seafoam
         │                │                 │
         │                └─ back ──▶ default theme restored
         │                                  │
         │                                  ▼
         │                              Settings
         │                              ├─ cache management
         │                              └─ provider status
         │
         └─ Theme Inspector available from any screen via pull-up sheet
```

### What This Demonstrates

1. **Instant creative expression**: Type anything, see a theme — the core value prop in one screen
2. **Dual light/dark from one generation**: Toggle system dark mode and the theme flips instantly — no second LLM call, both modes are complementary
3. **Keyed caching**: First visit generates, second visit is instant
4. **Two-level cache**: Hot themes from memory (nanoseconds), cold themes from disk (~1-5ms)
5. **Contextual theming**: Different parts of the app have different themes
6. **Full M3 coverage**: Every component recolors — not just backgrounds
7. **Prefetching**: Home feed preloads themes for visible communities
8. **Scoped themes**: Community theme doesn't leak to the home feed or playground
9. **Graceful loading**: Shimmer/skeleton while theme resolves on first visit
10. **Cache management**: Settings shows what's cached and allows clearing

### Demo Script (for a conference talk or video)

1. Open app — Playground screen, blank white, text field says "Describe a vibe..."
2. Type **"rainy tokyo night"** — hit enter
3. Loading shimmer, then: dark indigo background fades in, neon accent colors, every component on screen recolors. Buttons, cards, text fields, chips, slider, nav bar — all themed.
4. **Toggle dark mode** — flips instantly to complementary light version. Same hues, bright backgrounds. Toggle back.
5. Pull up Theme Inspector — show full 49-color grid, both light and dark side by side
6. Tap **"Save to communities"** — now it's a card in the Communities tab
7. Switch to Communities tab — see "Rainy Tokyo Night" alongside the preset communities
8. Tap **"Coffee"** — warm brown theme loads (first visit, brief shimmer)
9. Go back, tap **"Coffee"** again — **instant**, no shimmer, from cache
10. Back to Playground — type **"grandmother's kitchen"** — warm creams, soft shapes, serif font mood
11. Open Settings — show cache has 3 entries (tokyo, coffee, grandmother), hit rate stats
12. Clear cache, go back to Coffee — shimmer again, regenerating from scratch

---

## 16. Open Source Strategy

### Repository Name
`halogen` — periodic table element 17, the reactive group that bonds with everything.

- **GitHub**: `github.com/mattmckenna/halogen`
- **Docs**: `https://halogen.mmckenna.me`
- **Maven**: `me.mmckenna.halogen:halogen-*`
- **Package**: `import halogen.*`

### Tagline
"Reactive theming for Compose Multiplatform."
"LLM-generated Material 3 themes. On-device with Gemini Nano on Android. Cloud-powered everywhere else. Just a Gradle dependency."

### README Structure
1. Hero GIF (Playground screen: type "rainy tokyo night" → theme materializes → dark mode toggle)
2. One-line description
3. Platform support badges (Android, iOS, Desktop, Web)
4. 5-line Quick Start code sample
5. Feature list
6. Architecture diagram
7. Provider guide (Nano, OpenAI, Claude, Ollama, BYO)
8. Custom extensions guide
9. Design Decisions — link to `DESIGN.md` covering:
   - Why pure Kotlin color science instead of `material-color-utilities`
   - Why the LLM provider is a separate artifact
   - Why themes are keyed, not global-only
   - Why seed colors expand to 49 roles instead of asking the LLM for all of them
10. API reference link (halogen.mmckenna.me)
11. Contributing guide
12. License

### `DESIGN.md` (ships in the repo root)

A standalone document explaining the non-obvious architectural decisions. This is for contributors, curious developers, and anyone evaluating the library. Each decision follows the format:

- **Context:** What problem we faced
- **Options considered:** What we evaluated (with pros/cons)
- **Decision:** What we chose and why
- **Consequences:** What this enables and what it costs

Decisions documented:

1. **Pure Kotlin color science over `material-color-utilities`** — The ~450-line investment that enables KMP. Explained in full in Section 13.3 of this plan.

2. **LLM as a pluggable provider, not a core dependency** — Why `halogen-provider-nano` is a separate artifact. Gemini Nano is Android-only; the library is not. No consumer should be forced to pull ML Kit if they're using OpenAI or running on iOS.

3. **Seed colors + expansion over full palette generation** — The LLM outputs 6 seed values, not 49 hex codes. This keeps output under Gemini Nano's 256-token limit, produces better color harmony via tonal palette math, and makes the library usable with smaller/weaker models.

4. **One LLM call = both light and dark themes** — The LLM generates a single theme identity with two neutral seeds (`neuL` for light backgrounds, `neuD` for dark backgrounds). Primary, secondary, tertiary, and error hues are shared — the tonal palette system maps them to correct brightness per mode. This means system dark mode toggle is instant (no second LLM call), both schemes are guaranteed harmonious, and the token cost is just one extra hex field (~15 tokens).

5. **Keyed theme cache as a first-class primitive** — Themes are stored by key (route, category, subreddit, brand). This makes contextual theming (different parts of an app have different themes) a core feature rather than an afterthought. The resolve chain (cache → server → LLM → default) means the LLM is only called once per key, ever.

6. **Wrapping MaterialTheme instead of replacing it** — `HalogenTheme` calls `MaterialTheme` internally. Every existing M3 component works without modification. Migration is one line of code. This was chosen over a custom design system because compatibility with the M3 ecosystem is more valuable than flexibility.

### Positioning
- Not a design tool — it's a library for shipping apps
- Not a Material Theme Builder replacement — it's a runtime complement
- Not Android-only — KMP from day one, Compose Multiplatform native
- Differentiator: pluggable LLM, keyed contextual themes, works across platforms
- Target audience: Kotlin developers building with Compose who want user-personalized theming

### Potential Conference Talk
"Halogen: LLM-Generated Theming for Compose Multiplatform" — perfect for droidcon, KotlinConf, Android Dev Summit, or Google I/O extended
