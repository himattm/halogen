# API Reference

Complete API surface for all five Halogen modules.

---

## halogen-core

Core primitives and contracts. Pure Kotlin - no platform or LLM dependencies.

### HalogenThemeSpec

The LLM output contract. One instance represents a complete theme identity (both light and dark modes).

```kotlin
@Serializable
data class HalogenThemeSpec(
    @SerialName("pri") val primary: String,           // Hex seed — brand color
    @SerialName("sec") val secondary: String,          // Hex seed — supporting color
    @SerialName("ter") val tertiary: String,           // Hex seed — accent color
    @SerialName("neuL") val neutralLight: String,      // Hex seed — light mode neutral
    @SerialName("neuD") val neutralDark: String,       // Hex seed — dark mode neutral
    @SerialName("err") val error: String,              // Hex seed — error color

    @SerialName("font") val fontMood: String,          // "modern"|"classic"|"playful"|"minimal"|"mono"
    @SerialName("hw") val headingWeight: Int,           // 100-900
    @SerialName("bw") val bodyWeight: Int,              // 100-900
    @SerialName("ls") val tightLetterSpacing: Boolean,

    @SerialName("cs") val cornerStyle: String,          // "sharp"|"rounded"|"pill"|"soft"
    @SerialName("cx") val cornerScale: Float,           // 0.0 - 2.0

    @SerialName("ext") val extensions: Map<String, String>? = null
)
```

**Expansion** is handled by `ThemeExpander`, not by `HalogenThemeSpec` directly:

| Method | Returns | Description |
|--------|---------|-------------|
| `ThemeExpander.expand(spec, config)` | `ExpandedTheme` | Full expansion: both color schemes, typography, shapes |
| `ThemeExpander.expandColors(spec, isDark, config)` | `HalogenColorScheme` | Expand seeds into 49 M3 color roles |
| `ThemeExpander.expandTypography(spec)` | `HalogenTypography` | Generate 15 M3 text styles |
| `ThemeExpander.expandShapes(spec)` | `HalogenShapes` | Generate 5 M3 shape sizes |

### HalogenLlmProvider

Interface for LLM providers.

```kotlin
interface HalogenLlmProvider {
    suspend fun generate(prompt: String): String
    suspend fun availability(): HalogenLlmAvailability
}
```

### HalogenLlmAvailability

```kotlin
enum class HalogenLlmAvailability {
    READY,
    INITIALIZING,
    UNAVAILABLE
}
```

### HalogenLlmException

```kotlin
class HalogenLlmException(
    message: String,
    cause: Throwable? = null,
    val isRetryable: Boolean = false
) : Exception(message, cause)
```

### HalogenColorScheme

All 49 Material 3 color roles as ARGB integers.

**Color groups:**

- **Primary** (9): `primary`, `onPrimary`, `primaryContainer`, `onPrimaryContainer`, `inversePrimary`, `primaryFixed`, `primaryFixedDim`, `onPrimaryFixed`, `onPrimaryFixedVariant`
- **Secondary** (9): `secondary`, `onSecondary`, `secondaryContainer`, `onSecondaryContainer`, `secondaryFixed`, `secondaryFixedDim`, `onSecondaryFixed`, `onSecondaryFixedVariant`
- **Tertiary** (9): `tertiary`, `onTertiary`, `tertiaryContainer`, `onTertiaryContainer`, `tertiaryFixed`, `tertiaryFixedDim`, `onTertiaryFixed`, `onTertiaryFixedVariant`
- **Error** (4): `error`, `onError`, `errorContainer`, `onErrorContainer`
- **Surface** (13): `surface`, `onSurface`, `surfaceVariant`, `onSurfaceVariant`, `surfaceTint`, `surfaceBright`, `surfaceDim`, `surfaceContainer`, `surfaceContainerHigh`, `surfaceContainerHighest`, `surfaceContainerLow`, `surfaceContainerLowest`, `background`, `onBackground`
- **Utility** (5): `inverseSurface`, `inverseOnSurface`, `outline`, `outlineVariant`, `scrim`

| Method | Returns | Description |
|--------|---------|-------------|
| `toM3()` | `ColorScheme` | Convert to Compose Material 3 `ColorScheme` |

### HalogenTypography

All 15 Material 3 text styles.

| Method | Returns | Description |
|--------|---------|-------------|
| `toM3()` | `Typography` | Convert to Compose Material 3 `Typography` |

### HalogenShapes

All 5 Material 3 shape sizes: `extraSmall`, `small`, `medium`, `large`, `extraLarge`.

| Method | Returns | Description |
|--------|---------|-------------|
| `toM3()` | `Shapes` | Convert to Compose Material 3 `Shapes` |

### HalogenDefaults

```kotlin
object HalogenDefaults {
    fun light(): HalogenThemeSpec         // M3 baseline light theme
    fun materialYou(): HalogenThemeSpec   // Alias for light()
}
```

### Color Science

Pure Kotlin implementations in `halogen.color`:

- `Cam16` - CAM16 color appearance model
- `ViewingConditions` - Standard observer conditions
- `ColorUtils` - ARGB/XYZ/LAB conversions, contrast ratio calculation
- `MathUtils` - Clamping, interpolation utilities

---

## halogen-compose

Compose Multiplatform UI components.

### HalogenTheme

```kotlin
@Composable
fun HalogenTheme(
    spec: HalogenThemeSpec? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    config: HalogenConfig = HalogenConfig.Default,
    content: @Composable () -> Unit
)
```

Wraps `MaterialTheme` with a Halogen theme. Expands the given `HalogenThemeSpec` into M3 color scheme, typography, and shapes. Both light and dark color schemes are expanded upfront so toggling dark mode is instant. Provides `HalogenExtensions` via `LocalHalogenExtensions`. Pass `null` for `spec` to use default Material 3 values.

### HalogenSettingsCard

```kotlin
@Composable
fun HalogenSettingsCard(
    onGenerate: (String) -> Unit,
    isLoading: Boolean = false,
    currentSpec: HalogenThemeSpec? = null,
    modifier: Modifier = Modifier
)
```

Drop-in settings component with text field, generate button, loading indicator, color preview strip, and reset button. The `onGenerate` callback receives the user's natural language input.

### Composition Locals

| Local | Type | Description |
|-------|------|-------------|
| `LocalHalogenExtensions` | `HalogenExtensions` | Custom extension values |

### HalogenExtensions

```kotlin
class HalogenExtensions(private val map: Map<String, String>) {
    operator fun get(key: String): String?
    fun toColor(key: String): Color?
    companion object { fun empty(): HalogenExtensions }
}
```

Access via `HalogenTheme.extensions`.

---

## halogen-engine

Orchestration engine: caching, provider management, prompt construction.

### HalogenEngine

The central coordinator. Created via `Halogen.Builder`.

**Theme resolution:**

| Method | Description |
|--------|-------------|
| `resolve(key: String, hint: String? = null): HalogenResult` | Resolve a theme: cache -> remoteThemes -> LLM provider -> default |
| `prefetch(key: String, hint: String? = null): HalogenResult` | Cache a theme without applying it |
| `regenerate(key: String, hint: String): HalogenResult` | Force re-generate, bypassing cache |
| `apply(key: String, spec: HalogenThemeSpec)` | Manually apply and cache a theme |
| `applyDefault()` | Revert to the default theme |
| `refresh(key: String, hint: String? = null): HalogenResult` | Evict and re-resolve a single key |

**Cache management:**

| Method | Description |
|--------|-------------|
| `evict(key: String)` | Remove a single cached theme |
| `evict(keys: Set<String>)` | Remove multiple cached themes |
| `clearCache()` | Remove all cached themes |

**Observability:**

| Property | Type | Description |
|----------|------|-------------|
| `activeTheme` | `StateFlow<HalogenThemeSpec?>` | Currently active theme |
| `activeKey` | `StateFlow<String?>` | Key of the currently active theme |
| `cachingEnabled` | `Boolean` | Toggle caching on/off |
| `config` | `HalogenConfig` | Color science configuration (mutable at runtime) |

### Halogen.Builder

```kotlin
Halogen.Builder()
    .provider(provider: HalogenLlmProvider)
    .remoteThemes(provider: HalogenRemoteThemes)
    .defaultTheme(spec: HalogenThemeSpec)
    .extensions(vararg extensions: HalogenExtension)
    .cache(cache: ThemeCache)
    .config(config: HalogenConfig)
    .scope(scope: CoroutineScope)
    .build(): HalogenEngine  // throws if no provider or remoteThemes configured
```

### HalogenResult

```kotlin
sealed class HalogenResult {
    data class Success(val spec: HalogenThemeSpec) : HalogenResult()
    data class Cached(val spec: HalogenThemeSpec) : HalogenResult()
    data class FromServer(val spec: HalogenThemeSpec) : HalogenResult()
    data class ParseError(val message: String, val rawResponse: String? = null) : HalogenResult()
    data object Unavailable : HalogenResult()
    data object QuotaExceeded : HalogenResult()

    val themeSpec: HalogenThemeSpec?   // Non-null for Success, Cached, FromServer
    val isSuccess: Boolean             // true when themeSpec != null

    fun getOrNull(): HalogenThemeSpec?
    fun getOrThrow(): HalogenThemeSpec

    fun <R> fold(onSuccess: (HalogenThemeSpec) -> R, onFailure: (HalogenResult) -> R): R
    fun onSuccess(block: (HalogenThemeSpec) -> Unit): HalogenResult
    fun onFailure(block: (HalogenResult) -> Unit): HalogenResult
}
```

### HalogenCache

```kotlin
object HalogenCache {
    fun memory(maxEntries: Int = 20): ThemeCache
    fun none(): ThemeCache
}
```

### HalogenRemoteThemes

Optional source for pre-built themes from your backend. Not an LLM - just a key-value lookup.

```kotlin
fun interface HalogenRemoteThemes {
    suspend fun fetchTheme(key: String): HalogenThemeSpec?
}
```

Checked after cache, before the LLM provider. Return `null` for unknown keys to let the LLM handle them, or throw to skip and continue to the LLM.

### ThemeCache

```kotlin
interface ThemeCache {
    suspend fun get(key: String): HalogenThemeSpec?
    suspend fun put(key: String, spec: HalogenThemeSpec, source: ThemeSource)
    suspend fun contains(key: String): Boolean
    suspend fun evict(key: String)
    suspend fun evict(keys: Set<String>)
    suspend fun clear()
    suspend fun keys(): Set<String>
    suspend fun size(): Int
    suspend fun entries(): List<ThemeCacheEntry>
    fun observeChanges(): Flow<CacheEvent>
}
```

---

## halogen-cache-room

Optional persistent cache backed by Room KMP. Requires the `halogen-cache-room` dependency:

```kotlin
implementation("me.mmckenna.halogen:halogen-cache-room:0.1.0")
```

Available on Android, iOS, and JVM. Not available on wasmJs.

### HalogenRoomCache

Factory object for creating Room-backed persistent caches.

```kotlin
object HalogenRoomCache {
    fun initialize(context: Context)   // Android only — call in Application.onCreate()
    fun create(config: RoomThemeCacheConfig = RoomThemeCacheConfig()): ThemeCache
}
```

| Method | Description |
|--------|-------------|
| `initialize(context)` | Android only. Initializes the Room database. Must be called before `create()`. |
| `create(config)` | Create a `RoomThemeCache` instance with the given configuration. |

### RoomThemeCacheConfig

```kotlin
data class RoomThemeCacheConfig(
    val maxEntries: Int = 100,
    val maxAge: Duration = Duration.INFINITE
)
```

| Parameter | Default | Description |
|-----------|---------|-------------|
| `maxEntries` | 100 | Maximum number of cached themes before oldest entries are evicted |
| `maxAge` | `Duration.INFINITE` | Maximum age of a cache entry before it is considered stale |

### RoomThemeCache

The `ThemeCache` implementation backed by Room KMP. Created via `HalogenRoomCache.create()` - not instantiated directly.

Implements the full `ThemeCache` interface (`get`, `put`, `contains`, `evict`, `clear`, `keys`, `size`, `entries`, `observeChanges`). Themes persist across process death and app restarts.

---

## halogen-provider-nano

Gemini Nano on-device AI provider. Android only.

### GeminiNanoProvider

```kotlin
class GeminiNanoProvider(
    temperature: Float = 0.2f,
    topK: Int = 10
) : HalogenLlmProvider
```

| Method | Description |
|--------|-------------|
| `generate(prompt: String): String` | Send prompt to Gemini Nano, return JSON response |
| `availability(): HalogenLlmAvailability` | Check model feature status |
| `downloadModel(): Flow<DownloadStatus>` | Trigger Nano model download if needed |
| `warmup()` | Pre-load model into memory to reduce first-inference latency |
| `close()` | Release resources held by the underlying GenerativeModel |
