# KMP OpenAI Provider via Ktor

## Summary

Replace the Android-only Retrofit/OkHttp/Gson OpenAI provider in `sample/` with a Kotlin Multiplatform implementation using Ktor + kotlinx-serialization in `sample-shared/commonMain`. This gives all sample-shared targets (JVM Desktop, WasmJs, iOS) access to a real LLM provider. The `sample/` Android app retains Gemini Nano as its focus and drops its OpenAI code.

## New Code

### `sample-shared/commonMain` - OpenAI Provider

**`me.mmckenna.halogen.sample.llms.openai.OpenAiProvider`**

Implements `HalogenLlmProvider`. Uses Ktor `HttpClient` with `ContentNegotiation` plugin (kotlinx-serialization-json).

```kotlin
class OpenAiProvider(
    private val apiKey: String,
    private val model: String = "gpt-4o-mini",
    var temperature: Float = 0.3f,
    var topP: Float? = null,
    var maxTokens: Int = 300,
) : HalogenLlmProvider
```

- `generate(prompt)` - POST to `https://api.openai.com/v1/chat/completions` with Bearer auth, returns `choices[0].message.content`
- `availability()` - `READY` if apiKey is non-blank, `UNAVAILABLE` otherwise
- Error handling: map Ktor `ResponseException` to `HalogenLlmException`, mark 5xx as retryable
- The `HttpClient` is lazily initialized. Platform engines are resolved automatically by Ktor (no expect/actual needed for the client itself).

**`me.mmckenna.halogen.sample.llms.openai.ChatModels`**

`@Serializable` data classes replacing the Gson-annotated ones:

```kotlin
@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Float = 0.7f,
    @SerialName("max_tokens") val maxTokens: Int = 300,
    @SerialName("top_p") val topP: Float? = null,
)

@Serializable
data class Message(val role: String, val content: String)

@Serializable
data class ChatResponse(val choices: List<Choice>)

@Serializable
data class Choice(val message: Message)
```

### `sample-shared/commonMain` - Environment Variable Access

**`me.mmckenna.halogen.sample.Platform`** (expect/actual)

```kotlin
// commonMain
expect fun getEnvVar(name: String): String?

// jvmMain
actual fun getEnvVar(name: String): String? = System.getenv(name)

// iosMain
actual fun getEnvVar(name: String): String? = platform.posix.getenv(name)?.toKStringFromUtf8()

// wasmJsMain
actual fun getEnvVar(name: String): String? = null  // no env vars in browser
```

### Wiring - `HalogenDemoState`

Update `HalogenDemoState.create()` to accept an optional API key:

```kotlin
companion object {
    fun create(scope: CoroutineScope, openAiApiKey: String? = null): HalogenDemoState {
        val builder = Halogen.Builder()
            .defaultTheme(HalogenDefaults.materialYou())
            .scope(scope)
            .tokenBudget(Int.MAX_VALUE)

        if (openAiApiKey != null) {
            builder.provider(OpenAiProvider(openAiApiKey))
        } else {
            builder.provider(DemoProvider())
        }

        return HalogenDemoState(engine = builder.build(), scope = scope)
    }
}
```

Each platform entry point reads the env var and passes it in:

```kotlin
// jvmMain/main.kt, iosMain/MainViewController.kt
val apiKey = getEnvVar("OPENAI_API_KEY")
val demoState = HalogenDemoState.create(scope, openAiApiKey = apiKey)
```

WasmJs will always get `null` and fall back to DemoProvider.

## Dependency Changes

### `gradle/libs.versions.toml` - Add Ktor

```toml
[versions]
ktor = "3.1.3"

[libraries]
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
ktor-client-js = { module = "io.ktor:ktor-client-js", version.ref = "ktor" }
```

### `sample-shared/build.gradle.kts` - Add Dependencies

```kotlin
plugins {
    // ... existing
    alias(libs.plugins.kotlin.serialization)  // ADD
}

sourceSets {
    commonMain.dependencies {
        // ... existing
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.content.negotiation)
        implementation(libs.ktor.serialization.kotlinx.json)
        implementation(libs.kotlinx.serialization.json)
    }
    jvmMain.dependencies {
        // ... existing
        implementation(libs.ktor.client.okhttp)
    }
    iosMain.dependencies {
        implementation(libs.ktor.client.darwin)
    }
    val wasmJsMain by getting {
        dependencies {
            implementation(libs.ktor.client.js)
        }
    }
}
```

### `sample/build.gradle.kts` - Remove Retrofit/OkHttp

Remove these lines:
```kotlin
implementation(libs.okhttp)
implementation(libs.okhttp.logging)
implementation(libs.retrofit)
implementation(libs.retrofit.gson)
```

Remove the `OPENAI_API_KEY` BuildConfig field.

## Removals from `sample/`

Delete the entire `llms/` package:
- `llms/LlmProviderChoice.kt`
- `llms/openai/ChatRequest.kt`
- `llms/openai/OpenAiProvider.kt`
- `llms/openai/OpenAiService.kt`

Update `HalogenAppViewModel.kt`:
- Remove OpenAI provider references
- Remove `LlmProviderChoice` switching logic
- Keep Gemini Nano as sole provider, DemoProvider as default when no API key

Update `sample/` Settings/Playground screens:
- Remove provider switcher UI (segmented button for Nano/OpenAI)
- Keep Nano status/download/warmup controls

## Behavior by Platform

| Platform | API Key Source | Provider |
|----------|---------------|----------|
| JVM Desktop | `System.getenv("OPENAI_API_KEY")` | OpenAiProvider (or DemoProvider if no key) |
| iOS | `platform.posix.getenv("OPENAI_API_KEY")` | OpenAiProvider (or DemoProvider if no key) |
| WasmJs (browser) | N/A (always null) | DemoProvider |
| Android (`sample/`) | N/A (Nano-focused) | GeminiNanoProvider (or DemoProvider if no key) |

## What Does NOT Change

- `HalogenLlmProvider` interface in halogen-core
- `HalogenEngine` resolve logic
- `halogen-provider-nano` module
- UI screens in `sample-shared` (Playground, Weather, Test Harness, Settings)
- `DemoProvider` implementation
