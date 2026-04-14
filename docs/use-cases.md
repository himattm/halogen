# Use Cases

Seven real-world scenarios showing what you can build with Halogen.

---

## 1. On-Device Theming with Gemini Nano

The flagship use case: a user types a prompt, and a full Material 3 theme generates **on-device** with zero network traffic.

```kotlin
// Initialize with Gemini Nano — no API key needed
val engine = Halogen.Builder()
    .provider(GeminiNanoProvider())
    .config(HalogenConfig.Vibrant)
    .build()

// User types "sunset beach party" → theme generates on-device
val result = engine.resolve(
    key = "user-theme",
    hint = "sunset beach party, warm oranges and ocean blues"
)
```

Wrap your UI to apply the result:

```kotlin
val spec by engine.activeTheme.collectAsState()

HalogenTheme(spec = spec) {
    // MaterialTheme.colorScheme, .typography, .shapes all update
    App()
}
```

!!! info "Device Requirements"
    Gemini Nano requires **Pixel 9+**, **Samsung Galaxy S24+**, or other devices with on-device Gemini support and a locked bootloader. On unsupported devices, `availability()` returns `UNAVAILABLE` - consider using a cloud provider instead.

!!! tip "Warm the model on launch"
    Call `GeminiNanoProvider.warmup()` in `onCreate` to pre-load the model into memory. First inference drops from ~2s to ~500ms.

---

## 2. Server-Driven Theme Hints

Your backend sends a natural language hint per screen or section. The app resolves it **once**, caches it, and every subsequent visit is a cache hit - no LLM call, no network.

```kotlin
// Your API response includes a theme hint
@Serializable
data class ScreenConfig(
    val title: String,
    val themeHint: String,  // e.g., "professional finance, dark navy, sharp corners"
    val content: List<ContentBlock>,
)
```

On navigation, resolve the hint:

```kotlin
// In your ViewModel or navigation handler
fun onScreenLoaded(config: ScreenConfig) {
    viewModelScope.launch {
        engine.resolve(
            key = "screen/${config.title}",
            hint = config.themeHint,
        )
    }
}
```

```kotlin
// In Compose — theme follows the engine's active spec
val spec by engine.activeTheme.collectAsState()

HalogenTheme(spec = spec) {
    ScreenContent(config)
}
```

The LLM runs once per unique key. After that, `resolve()` returns `HalogenResult.Cached` instantly - even across app restarts if you use a persistent cache.

---

## 3. Server-Provided Complete Themes (No LLM)

Skip the LLM entirely. Your backend provides a full `HalogenThemeSpec` as JSON: a design admin curates themes in a CMS, and the app applies them directly.

```kotlin
// Backend returns complete theme JSON
val themeJson = """
{
  "pri": "#1B6B4A", "sec": "#4A6B5C", "ter": "#6B4A1B",
  "neuL": "#F5F5F0", "neuD": "#1A1A1A",
  "err": "#B3261E", "font": "geometric", "hw": 700,
  "bw": 400, "ls": false, "cs": "rounded", "cx": 1.0
}
"""

// Parse and apply — no LLM provider needed
val spec = HalogenThemeSpec.fromJson(themeJson)
engine.apply(key = "brand-spring-2025", spec = spec)
```

!!! tip "Best for brand-controlled apps"
    Use `engine.apply()` when you need pixel-perfect design control. The spec is cached as `ThemeSource.MANUAL`, so it behaves identically to LLM-generated themes for caching and retrieval.

You can also wire up `HalogenRemoteThemes` so the engine fetches pre-built themes automatically during resolve:

```kotlin
val engine = Halogen.Builder()
    .remoteThemes { key ->
        // Fetch HalogenThemeSpec from your API by key
        api.fetchTheme(key)
    }
    .build()

// Remote themes resolve automatically — no LLM needed
engine.resolve(key = "brand-spring-2025")
```

---

## 4. Contextual / Environmental Theming

Theme adapts to real-world context automatically. Combine sensor data or system state with Halogen hints to create themes that feel alive.

=== "Weather-Based"

    ```kotlin
    fun weatherHint(weather: Weather): String = when (weather) {
        Weather.SUNNY   -> "bright sunny day, warm yellows, cheerful energy"
        Weather.RAINY   -> "rainy afternoon, cool grays and muted blues"
        Weather.STORMY  -> "dramatic thunderstorm, dark purples, electric highlights"
        Weather.SNOWY   -> "fresh snowfall, crisp whites and icy blues"
        Weather.CLOUDY  -> "overcast sky, soft neutral tones"
    }

    // Resolve with weather context
    val weather = weatherService.current()
    engine.resolve(
        key = "weather/${weather.name.lowercase()}",
        hint = weatherHint(weather),
    )
    ```

=== "Time of Day"

    ```kotlin
    fun timeOfDayHint(hour: Int): String = when (hour) {
        in 5..7   -> "soft dawn, gentle warm pastels, new beginnings"
        in 8..11  -> "bright morning, clean and energetic"
        in 12..16 -> "midday sun, vivid and confident"
        in 17..19 -> "golden hour dusk, amber and warm shadows"
        else      -> "deep night, dark cool tones, calm"
    }

    engine.resolve(
        key = "time/${if (hour in 5..19) "day" else "night"}",
        hint = timeOfDayHint(LocalTime.now().hour),
    )
    ```

=== "Battery-Aware"

    ```kotlin
    fun batteryAwareResolve(engine: HalogenEngine, level: Int) {
        viewModelScope.launch {
            if (level <= 15) {
                // Low battery → dark OLED-friendly theme, minimal saturation
                engine.config = HalogenConfig.Muted
                engine.resolve(
                    key = "battery/low",
                    hint = "pure dark OLED theme, true blacks, minimal color"
                )
            } else {
                engine.config = HalogenConfig.Default
                engine.resolve(key = "battery/normal", hint = "balanced default theme")
            }
        }
    }
    ```

!!! note "Cache keys are your friend"
    Each context variant gets its own cache key (`weather/sunny`, `time/night`, `battery/low`). The LLM generates once per variant - after that, context switches are instant cache hits.

---

## 5. Per-Content Community Theming

Each community, channel, or category gets its own generated theme. Prefetch on a list screen so themes are ready before the user taps.

```kotlin
// Prefetch themes for visible communities — no UI change, just warms cache
fun onCommunityListVisible(communities: List<Community>) {
    scope.launch {
        communities.forEach { community ->
            engine.prefetch(
                key = "community/${community.id}",
                hint = community.description,  // "retro gaming nostalgia, pixel art vibes"
            )
        }
    }
}
```

Then apply per-screen:

```kotlin
// On navigation to a community
fun onCommunitySelected(community: Community) {
    viewModelScope.launch {
        engine.resolve(
            key = "community/${community.id}",
            hint = community.description,
        )
    }
}

// In Compose
val spec by engine.activeTheme.collectAsState()

HalogenTheme(spec = spec) {
    CommunityScreen(community)
}
```

!!! tip "Prefetch is fire-and-forget"
    `prefetch()` populates the cache without changing the active theme. When the user navigates, `resolve()` hits the cache and applies instantly.

---

## 6. Config Presets for Brand Control

`HalogenConfig` presets control the **visual intensity** of generated themes without changing the LLM provider or prompt. Same hint, different feel.

```kotlin
// Same prompt, different presets — dramatically different results
val presets = listOf(
    "Vibrant"    to HalogenConfig.Vibrant,    // Bold, saturated, eye-catching
    "Muted"      to HalogenConfig.Muted,      // Corporate, calm, understated
    "Pastel"     to HalogenConfig.Pastel,      // Soft, gentle, airy
    "Punchy"     to HalogenConfig.Punchy,      // High-energy, gaming, sports
    "Editorial"  to HalogenConfig.Editorial,   // One strong color, neutral rest
    "Expressive" to HalogenConfig.Expressive,  // Colorful everywhere, even surfaces
    "Monochrome" to HalogenConfig.Monochrome,  // Single hue at different intensities
)

// Switch preset at runtime — next resolve uses the new config
engine.config = HalogenConfig.Pastel
engine.resolve(key = "ocean/pastel", hint = "deep ocean, coral reef")
```

The preset map is also available for building UI pickers:

```kotlin
// Build a preset selector from the built-in map
HalogenConfig.presets.forEach { (name, config) ->
    PresetChip(
        label = name,
        selected = engine.config == config,
        onClick = { engine.config = config },
    )
}
```

!!! info "How presets work"
    Presets control **chroma caps** - how saturated the expanded M3 palette is allowed to be. The LLM still picks hues and relationships; the preset shapes the final intensity. `Vibrant` allows primary chroma up to 64, while `Muted` caps at 36. Each preset also includes `promptGuidance` that steers the LLM toward style-appropriate color choices.

---

## Combining Patterns

These patterns compose naturally. A real app might use several together:

```kotlin
val engine = Halogen.Builder()
    .provider(GeminiNanoProvider())              // On-device generation
    .remoteThemes { key -> api.theme(key) }     // Pre-built themes from backend
    .config(HalogenConfig.Vibrant)               // Brand preset
    .build()

// Remote themes + on-device generation + prefetching
communities.forEach { engine.prefetch("c/${it.id}", it.themeHint) }
```

---

## 7. Image-Based Theming

Theme your app based on visual content — album art, profile photos, product images, or any URL.

### One-Line URL Resolution

The simplest path: load an image, extract colors, and resolve a theme in one call.

```kotlin
val engine = Halogen.Builder()
    .provider(OpenAiProvider(apiKey = BuildConfig.OPENAI_KEY))
    .cache(HalogenCache.memory())
    .build()

// Album art URL → full Material 3 theme
val result = engine.resolveImage(
    url = album.artUrl,
    imageLoader = imageLoader,
    context = context,
)
```

The URL acts as the cache key, so subsequent calls for the same image return instantly.

### Algorithmic (No LLM)

For instant theming without an LLM call, extract colors and convert directly:

```kotlin
val colors = extractColors(album.artUrl, imageLoader, context)
if (colors != null) {
    val spec = colors.toSpec()  // algorithmic — instant
    engine.apply("album:${album.id}", spec)
}
```

`toSpec()` maps extracted colors to theme seed roles based on HCT properties (chroma for vibrancy, hue for variety, tone for lightness). No network call, no LLM.

### LLM-Enhanced

For richer results, let the LLM interpret the palette with mood awareness:

```kotlin
// resolveImage() uses toHint() internally — the LLM sees the palette
// plus mood descriptors like "dark, vibrant" and picks typography/shapes to match
engine.resolveImage(album.artUrl, imageLoader, context)
```

The LLM receives a prompt like:
> Image palette: #2A1B3D (45%), #E94560 (30%), #533483 (25%). Dark, vibrant mood.

And generates a complete theme with matching typography and corner styles.

### Raw Pixels (Non-Coil)

If you have pixel data from a custom image loader:

```kotlin
val result = engine.resolveImage(
    key = "album:${album.id}",
    pixels = argbPixels,
    width = imageWidth,
    height = imageHeight,
)
```

This overload is pure common code with no Coil dependency.
