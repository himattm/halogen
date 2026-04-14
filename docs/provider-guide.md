# Provider Guide

How to use built-in providers and implement cloud providers.

---

## HalogenLlmProvider Interface

Every LLM provider implements a single interface from `halogen-core`:

```kotlin
interface HalogenLlmProvider {

    /**
     * Generate a HalogenThemeSpec JSON string from a prompt.
     *
     * The engine builds the full prompt (system instructions, few-shot
     * examples, extensions, user hint). The provider just sends it
     * to the LLM and returns the raw JSON response.
     */
    suspend fun generate(prompt: String): String

    /**
     * Check if this provider is currently available.
     */
    suspend fun availability(): HalogenLlmAvailability
}

enum class HalogenLlmAvailability {
    READY,           // Can generate right now
    INITIALIZING,    // Downloading model, warming up
    UNAVAILABLE      // Not supported / no API key / no network
}
```

The provider is intentionally simple: it receives a fully-formed prompt and returns raw LLM output. The engine handles prompt construction, JSON parsing, validation, and caching.

---

## [Gemini Nano](https://developer.android.com/ai/gemini-nano) Setup (Android)

### 1. Add the provider dependency

```kotlin
// build.gradle.kts
dependencies {
    implementation("me.mmckenna.halogen:halogen-provider-nano:0.2.0")
}
```

### 2. Initialize the provider

```kotlin
val nanoProvider = GeminiNanoProvider(
    temperature = 0.2f,  // Low temperature for consistent themes
    topK = 10
)
```

### 3. Check availability

```kotlin
when (nanoProvider.availability()) {
    HalogenLlmAvailability.READY -> { /* Good to go */ }
    HalogenLlmAvailability.INITIALIZING -> {
        // Model is downloading — collect the download flow
        nanoProvider.downloadModel().collect { status ->
            // Update UI with download progress
        }
    }
    HalogenLlmAvailability.UNAVAILABLE -> {
        // Device doesn't support Nano — consider using a cloud provider instead
    }
}
```

### Device requirements

- Pixel 9+, Samsung Galaxy S24+, or other devices with AICore ([ML Kit](https://developers.google.com/ml-kit))
- Locked bootloader required
- Foreground-only inference
- Per-app inference quota (mitigated by caching)

---

## Cloud Provider Examples

### OpenAI

```kotlin
class OpenAiProvider(
    private val apiKey: String,
    private val model: String = "gpt-4o-mini",
    private val client: OkHttpClient = OkHttpClient(),
) : HalogenLlmProvider {

    override suspend fun generate(prompt: String): String {
        val body = """
            {"model":"$model",
             "messages":[{"role":"user","content":${prompt.toJsonString()}}],
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

### Claude

```kotlin
class ClaudeProvider(
    private val apiKey: String,
    private val model: String = "claude-sonnet-4-20250514",
) : HalogenLlmProvider {

    override suspend fun generate(prompt: String): String {
        // POST to https://api.anthropic.com/v1/messages
        // Return the content text from the response
    }

    override suspend fun availability(): HalogenLlmAvailability {
        return if (apiKey.isNotBlank()) HalogenLlmAvailability.READY
               else HalogenLlmAvailability.UNAVAILABLE
    }
}
```

### Ollama (Local)

```kotlin
class OllamaProvider(
    private val baseUrl: String = "http://localhost:11434",
    private val model: String = "llama3.2",
) : HalogenLlmProvider {

    override suspend fun generate(prompt: String): String {
        // POST to $baseUrl/api/generate
        // Return the response text
    }

    override suspend fun availability(): HalogenLlmAvailability {
        return try {
            // GET $baseUrl/api/tags — if it responds, Ollama is running
            HalogenLlmAvailability.READY
        } catch (e: Exception) {
            HalogenLlmAvailability.UNAVAILABLE
        }
    }
}
```

---

## Single Provider Model

The engine uses a single `HalogenLlmProvider`. Pick the one that fits your use case:

```kotlin
// On-device with Gemini Nano (Android)
val halogen = Halogen.Builder()
    .provider(GeminiNanoProvider())
    .cache(HalogenCache.memory())
    .build()

// Cloud provider (cross-platform)
val halogen = Halogen.Builder()
    .provider(OpenAiProvider(apiKey = BuildConfig.OPENAI_KEY))
    .cache(HalogenCache.memory())
    .build()
```

### What happens when the provider is unavailable?

The engine calls `availability()` before every `generate()` call. With a single provider, there's no fallback chain:

| `availability()` returns | Engine behavior |
|--------------------------|----------------|
| `READY` | Calls `generate()` normally |
| `INITIALIZING` | Waits up to 2 minutes, polling every 2 seconds. If it becomes `READY`, proceeds. If it times out, returns `HalogenResult.Unavailable`. |
| `UNAVAILABLE` | Returns `HalogenResult.Unavailable` immediately |

If `generate()` throws a `HalogenLlmException`, the engine returns `HalogenResult.Unavailable`. Your app should handle this gracefully, for example by showing the `defaultTheme` or a retry button.

!!! tip "Always set a default theme"
    Call `.defaultTheme(HalogenDefaults.light())` on the Builder so your app has a usable theme even when the provider is unavailable.

### Resolve chain

When you call `resolve(key, hint)`, the engine tries each source in order:

```
1. Cache         - instant, no network
2. Remote themes - pre-built themes from your backend (if configured)
3. LLM provider  - generates a new theme from the hint
4. Default theme  - static fallback (if configured)
```

Remote themes and the LLM provider serve different purposes. Remote themes fetch pre-built themes by key (no LLM involved). The LLM provider generates new themes from natural language. If you configure both, remote themes are checked first - this lets you curate themes for popular keys while the LLM handles everything else.

You can configure either or both:

```kotlin
// LLM only
Halogen.Builder()
    .provider(GeminiNanoProvider())
    .build()

// Remote themes only (no LLM, no generation)
Halogen.Builder()
    .remoteThemes { key -> api.fetchTheme(key) }
    .build()

// Both: remote themes for known keys, LLM for the rest
Halogen.Builder()
    .provider(OpenAiProvider(apiKey))
    .remoteThemes { key -> api.fetchTheme(key) }
    .build()
```

---

## Writing Your Own Provider

Implement `HalogenLlmProvider` and you're done. The contract is minimal:

1. **`generate(prompt)`** - Send the prompt string to your LLM, return the raw response. The engine handles JSON extraction and parsing.
2. **`availability()`** - Return `READY`, `INITIALIZING`, or `UNAVAILABLE`. The engine checks this before calling `generate()`.

!!! tip "Keep providers simple"
    The provider should be a thin pipe to the LLM. Don't parse JSON, validate colors, or manage caching in the provider - the engine does all of that.

```kotlin
class MyCustomProvider : HalogenLlmProvider {

    override suspend fun generate(prompt: String): String {
        // 1. Send prompt to your LLM
        // 2. Return the raw text response
        // The engine will extract and parse the JSON
    }

    override suspend fun availability(): HalogenLlmAvailability {
        // Return READY if the provider can generate right now
        return HalogenLlmAvailability.READY
    }
}
```

### Error handling

Throw `HalogenLlmException` for failures:

```kotlin
throw HalogenLlmException(
    message = "API returned 429",
    cause = httpException,
    isRetryable = true  // Engine will retry once, then return default theme
)
```
