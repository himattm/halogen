# KMP OpenAI Provider Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the Android-only Retrofit/OkHttp OpenAI provider with a KMP Ktor implementation in `sample-shared`, and strip OpenAI code from the Android `sample/` app (keeping it as a Gemini Nano showcase).

**Architecture:** New `OpenAiProvider` in `sample-shared/commonMain` using Ktor + kotlinx-serialization. Platform entry points read `OPENAI_API_KEY` from env var via expect/actual. `sample/` Android app drops all OpenAI code, keeps Gemini Nano as its sole provider.

**Tech Stack:** Ktor 3.1.3, kotlinx-serialization-json 1.8.1, Kotlin 2.2.20, Compose Multiplatform 1.10.1

---

## File Map

**Create:**
- `sample-shared/src/commonMain/kotlin/me/mmckenna/halogen/sample/llms/openai/OpenAiProvider.kt` - Ktor-based HalogenLlmProvider
- `sample-shared/src/commonMain/kotlin/me/mmckenna/halogen/sample/llms/openai/ChatModels.kt` - @Serializable request/response data classes
- `sample-shared/src/commonMain/kotlin/me/mmckenna/halogen/sample/Platform.kt` - expect fun getEnvVar
- `sample-shared/src/jvmMain/kotlin/me/mmckenna/halogen/sample/Platform.jvm.kt` - actual via System.getenv
- `sample-shared/src/iosMain/kotlin/me/mmckenna/halogen/sample/Platform.ios.kt` - actual via platform.posix.getenv
- `sample-shared/src/wasmJsMain/kotlin/me/mmckenna/halogen/sample/Platform.wasmJs.kt` - actual returns null

**Modify:**
- `gradle/libs.versions.toml` - Add Ktor version + library entries
- `sample-shared/build.gradle.kts` - Add serialization plugin + Ktor deps per source set
- `sample-shared/src/commonMain/kotlin/me/mmckenna/halogen/sample/HalogenDemoState.kt` - Accept optional API key, wire OpenAI or DemoProvider
- `sample-shared/src/jvmMain/kotlin/me/mmckenna/halogen/sample/main.kt` - Read env var, pass to HalogenDemoState
- `sample-shared/src/iosMain/kotlin/me/mmckenna/halogen/sample/MainViewController.kt` - Read env var, pass to HalogenDemoState
- `sample-shared/src/wasmJsMain/kotlin/me/mmckenna/halogen/sample/main.kt` - Pass null (DemoProvider only)
- `sample/build.gradle.kts` - Remove Retrofit/OkHttp/Gson deps + BuildConfig field
- `sample/src/main/kotlin/me/mmckenna/halogen/sample/HalogenAppViewModel.kt` - Remove OpenAI refs, Nano-only
- `sample/src/main/kotlin/me/mmckenna/halogen/sample/HalogenDemoApp.kt` - Remove provider switcher params
- `sample/src/main/kotlin/me/mmckenna/halogen/sample/screens/settings/SettingsScreen.kt` - Remove provider switcher UI
- `sample/src/main/kotlin/me/mmckenna/halogen/sample/screens/playground/PlaygroundScreen.kt` - Remove LlmProviderChoice refs
- `sample/src/main/kotlin/me/mmckenna/halogen/sample/screens/playground/PlaygroundViewModel.kt` - Remove OpenAI tuning sync

**Delete:**
- `sample/src/main/kotlin/me/mmckenna/halogen/sample/llms/LlmProviderChoice.kt`
- `sample/src/main/kotlin/me/mmckenna/halogen/sample/llms/openai/ChatRequest.kt`
- `sample/src/main/kotlin/me/mmckenna/halogen/sample/llms/openai/OpenAiProvider.kt`
- `sample/src/main/kotlin/me/mmckenna/halogen/sample/llms/openai/OpenAiService.kt`

---

### Task 1: Add Ktor dependencies to version catalog and sample-shared build

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `sample-shared/build.gradle.kts`

- [ ] **Step 1: Add Ktor version and library entries to version catalog**

In `gradle/libs.versions.toml`, add the ktor version after the `retrofit` line in `[versions]`:

```toml
ktor = "3.1.3"
```

Add library entries after the `# OkHttp / Retrofit` block in `[libraries]`:

```toml
# Ktor (KMP HTTP client)
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
ktor-client-js = { module = "io.ktor:ktor-client-js", version.ref = "ktor" }
```

- [ ] **Step 2: Add serialization plugin and Ktor deps to sample-shared build**

In `sample-shared/build.gradle.kts`, add the serialization plugin:

```kotlin
plugins {
    id("halogen.kmp-library")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}
```

Add Ktor dependencies to the `sourceSets` block. The full `sourceSets` block becomes:

```kotlin
    sourceSets {
        commonMain.dependencies {
            implementation(project(":halogen-core"))
            implementation(project(":halogen-compose"))
            implementation(project(":halogen-engine"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
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

- [ ] **Step 3: Verify Gradle sync succeeds**

Run: `./gradlew -q --console=plain :sample-shared:dependencies --configuration commonMainImplementationDependenciesMetadata | head -30`

Expected: Output includes `io.ktor:ktor-client-core:3.1.3` and `org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1`.

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml sample-shared/build.gradle.kts
git commit -m "build: add Ktor + kotlinx-serialization deps to sample-shared for KMP OpenAI provider"
```

---

### Task 2: Create expect/actual getEnvVar for platform env var access

**Files:**
- Create: `sample-shared/src/commonMain/kotlin/me/mmckenna/halogen/sample/Platform.kt`
- Create: `sample-shared/src/jvmMain/kotlin/me/mmckenna/halogen/sample/Platform.jvm.kt`
- Create: `sample-shared/src/iosMain/kotlin/me/mmckenna/halogen/sample/Platform.ios.kt`
- Create: `sample-shared/src/wasmJsMain/kotlin/me/mmckenna/halogen/sample/Platform.wasmJs.kt`

- [ ] **Step 1: Create the expect declaration in commonMain**

Create `sample-shared/src/commonMain/kotlin/me/mmckenna/halogen/sample/Platform.kt`:

```kotlin
package me.mmckenna.halogen.sample

expect fun getEnvVar(name: String): String?
```

- [ ] **Step 2: Create the JVM actual**

Create `sample-shared/src/jvmMain/kotlin/me/mmckenna/halogen/sample/Platform.jvm.kt`:

```kotlin
package me.mmckenna.halogen.sample

actual fun getEnvVar(name: String): String? = System.getenv(name)
```

- [ ] **Step 3: Create the iOS actual**

Create `sample-shared/src/iosMain/kotlin/me/mmckenna/halogen/sample/Platform.ios.kt`:

```kotlin
package me.mmckenna.halogen.sample

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKStringFromUtf8
import platform.posix.getenv

@OptIn(ExperimentalForeignApi::class)
actual fun getEnvVar(name: String): String? = getenv(name)?.toKStringFromUtf8()
```

- [ ] **Step 4: Create the WasmJs actual**

Create `sample-shared/src/wasmJsMain/kotlin/me/mmckenna/halogen/sample/Platform.wasmJs.kt`:

```kotlin
package me.mmckenna.halogen.sample

actual fun getEnvVar(name: String): String? = null
```

- [ ] **Step 5: Verify compilation**

Run: `./gradlew -q --console=plain :sample-shared:compileKotlinJvm`

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add sample-shared/src/commonMain/kotlin/me/mmckenna/halogen/sample/Platform.kt \
       sample-shared/src/jvmMain/kotlin/me/mmckenna/halogen/sample/Platform.jvm.kt \
       sample-shared/src/iosMain/kotlin/me/mmckenna/halogen/sample/Platform.ios.kt \
       sample-shared/src/wasmJsMain/kotlin/me/mmckenna/halogen/sample/Platform.wasmJs.kt
git commit -m "feat: add expect/actual getEnvVar for KMP env var access"
```

---

### Task 3: Create KMP OpenAI provider using Ktor

**Files:**
- Create: `sample-shared/src/commonMain/kotlin/me/mmckenna/halogen/sample/llms/openai/ChatModels.kt`
- Create: `sample-shared/src/commonMain/kotlin/me/mmckenna/halogen/sample/llms/openai/OpenAiProvider.kt`

- [ ] **Step 1: Create serializable data models**

Create `sample-shared/src/commonMain/kotlin/me/mmckenna/halogen/sample/llms/openai/ChatModels.kt`:

```kotlin
package me.mmckenna.halogen.sample.llms.openai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Float = 0.7f,
    @SerialName("max_tokens") val maxTokens: Int = 300,
    @SerialName("top_p") val topP: Float? = null,
)

@Serializable
internal data class Message(val role: String, val content: String)

@Serializable
internal data class ChatResponse(val choices: List<Choice>)

@Serializable
internal data class Choice(val message: Message)
```

- [ ] **Step 2: Create the Ktor-based OpenAiProvider**

Create `sample-shared/src/commonMain/kotlin/me/mmckenna/halogen/sample/llms/openai/OpenAiProvider.kt`:

```kotlin
package me.mmckenna.halogen.sample.llms.openai

import halogen.HalogenLlmAvailability
import halogen.HalogenLlmException
import halogen.HalogenLlmProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class OpenAiProvider(
    private val apiKey: String,
    private val model: String = "gpt-4o-mini",
    var temperature: Float = 0.3f,
    var topP: Float? = null,
    var maxTokens: Int = 300,
) : HalogenLlmProvider {

    private val client by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    override suspend fun generate(prompt: String): String {
        val request = ChatRequest(
            model = model,
            messages = listOf(Message(role = "user", content = prompt)),
            temperature = temperature,
            maxTokens = maxTokens,
            topP = topP,
        )
        val response = try {
            client.post("https://api.openai.com/v1/chat/completions") {
                contentType(ContentType.Application.Json)
                bearerAuth(apiKey)
                setBody(request)
            }
        } catch (e: Exception) {
            throw HalogenLlmException("OpenAI request failed: ${e.message}", e, isRetryable = true)
        }
        if (!response.status.isSuccess()) {
            val body = response.bodyAsText()
            throw HalogenLlmException(
                "OpenAI API error ${response.status.value}: $body",
                isRetryable = response.status.value >= 500,
            )
        }
        val chatResponse = response.body<ChatResponse>()
        return chatResponse.choices.first().message.content
    }

    override suspend fun availability(): HalogenLlmAvailability {
        return if (apiKey.isNotBlank()) {
            HalogenLlmAvailability.READY
        } else {
            HalogenLlmAvailability.UNAVAILABLE
        }
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew -q --console=plain :sample-shared:compileKotlinJvm`

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add sample-shared/src/commonMain/kotlin/me/mmckenna/halogen/sample/llms/openai/ChatModels.kt \
       sample-shared/src/commonMain/kotlin/me/mmckenna/halogen/sample/llms/openai/OpenAiProvider.kt
git commit -m "feat: add KMP OpenAI provider using Ktor + kotlinx-serialization"
```

---

### Task 4: Wire OpenAiProvider into HalogenDemoState and platform entry points

**Files:**
- Modify: `sample-shared/src/commonMain/kotlin/me/mmckenna/halogen/sample/HalogenDemoState.kt`
- Modify: `sample-shared/src/jvmMain/kotlin/me/mmckenna/halogen/sample/main.kt`
- Modify: `sample-shared/src/iosMain/kotlin/me/mmckenna/halogen/sample/MainViewController.kt`
- Modify: `sample-shared/src/wasmJsMain/kotlin/me/mmckenna/halogen/sample/main.kt`

- [ ] **Step 1: Update HalogenDemoState.create() to accept an API key**

Replace the full content of `sample-shared/src/commonMain/kotlin/me/mmckenna/halogen/sample/HalogenDemoState.kt`:

```kotlin
package me.mmckenna.halogen.sample

import halogen.HalogenDefaults
import halogen.engine.Halogen
import halogen.engine.HalogenEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.mmckenna.halogen.sample.llms.openai.OpenAiProvider

class HalogenDemoState(
    val engine: HalogenEngine,
    val scope: CoroutineScope,
) {
    private val _darkOverride = MutableStateFlow<Boolean?>(null)
    val darkOverride: StateFlow<Boolean?> = _darkOverride.asStateFlow()

    fun toggleDarkMode(currentIsDark: Boolean) {
        _darkOverride.value = !currentIsDark
    }

    companion object {
        fun create(scope: CoroutineScope, openAiApiKey: String? = null): HalogenDemoState {
            val builder = Halogen.Builder()
                .defaultTheme(HalogenDefaults.materialYou())
                .scope(scope)
                .tokenBudget(Int.MAX_VALUE)

            if (!openAiApiKey.isNullOrBlank()) {
                builder.provider(OpenAiProvider(openAiApiKey))
            } else {
                builder.provider(DemoProvider())
            }

            return HalogenDemoState(engine = builder.build(), scope = scope)
        }
    }
}
```

- [ ] **Step 2: Update JVM Desktop entry point**

Replace the full content of `sample-shared/src/jvmMain/kotlin/me/mmckenna/halogen/sample/main.kt`:

```kotlin
package me.mmckenna.halogen.sample

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun main() = application {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val apiKey = getEnvVar("OPENAI_API_KEY")

    Window(
        onCloseRequest = ::exitApplication,
        title = "Halogen Playground",
    ) {
        HalogenDemoApp(
            demoState = HalogenDemoState.create(scope, openAiApiKey = apiKey),
        )
    }
}
```

- [ ] **Step 3: Update iOS entry point**

Replace the full content of `sample-shared/src/iosMain/kotlin/me/mmckenna/halogen/sample/MainViewController.kt`:

```kotlin
package me.mmckenna.halogen.sample

import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun MainViewController() = ComposeUIViewController {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val apiKey = getEnvVar("OPENAI_API_KEY")

    HalogenDemoApp(
        demoState = HalogenDemoState.create(scope, openAiApiKey = apiKey),
    )
}
```

- [ ] **Step 4: Update WasmJs entry point**

Replace the full content of `sample-shared/src/wasmJsMain/kotlin/me/mmckenna/halogen/sample/main.kt`:

```kotlin
package me.mmckenna.halogen.sample

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    ComposeViewport(document.body!!) {
        HalogenDemoApp(
            demoState = HalogenDemoState.create(scope),
        )
    }
}
```

- [ ] **Step 5: Verify JVM compilation**

Run: `./gradlew -q --console=plain :sample-shared:compileKotlinJvm`

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add sample-shared/src/commonMain/kotlin/me/mmckenna/halogen/sample/HalogenDemoState.kt \
       sample-shared/src/jvmMain/kotlin/me/mmckenna/halogen/sample/main.kt \
       sample-shared/src/iosMain/kotlin/me/mmckenna/halogen/sample/MainViewController.kt \
       sample-shared/src/wasmJsMain/kotlin/me/mmckenna/halogen/sample/main.kt
git commit -m "feat: wire OpenAI provider into sample-shared entry points with env var"
```

---

### Task 5: Remove OpenAI code from Android sample app

**Files:**
- Delete: `sample/src/main/kotlin/me/mmckenna/halogen/sample/llms/LlmProviderChoice.kt`
- Delete: `sample/src/main/kotlin/me/mmckenna/halogen/sample/llms/openai/ChatRequest.kt`
- Delete: `sample/src/main/kotlin/me/mmckenna/halogen/sample/llms/openai/OpenAiProvider.kt`
- Delete: `sample/src/main/kotlin/me/mmckenna/halogen/sample/llms/openai/OpenAiService.kt`
- Modify: `sample/build.gradle.kts`
- Modify: `sample/src/main/kotlin/me/mmckenna/halogen/sample/HalogenAppViewModel.kt`

- [ ] **Step 1: Delete the llms/ package**

```bash
rm -rf sample/src/main/kotlin/me/mmckenna/halogen/sample/llms
```

- [ ] **Step 2: Remove Retrofit/OkHttp/Gson deps and BuildConfig field from sample build**

In `sample/build.gradle.kts`, remove these 4 lines from the `dependencies` block:

```kotlin
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
```

Also remove the `buildConfigField` line from `defaultConfig`:

```kotlin
        buildConfigField("String", "OPENAI_API_KEY", "\"${localProps.getProperty("OPENAI_API_KEY", "")}\"")
```

And remove the `localProps` block at the top (lines 7-10):

```kotlin
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}
```

And the `import java.util.Properties` on line 7.

The full `sample/build.gradle.kts` should become:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "me.mmckenna.halogen.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = "me.mmckenna.halogen.sample"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":halogen-core"))
    implementation(project(":halogen-compose"))
    implementation(project(":halogen-engine"))
    implementation(project(":halogen-provider-nano"))
    implementation(project(":halogen-cache-room"))
    implementation(libs.mlkit.genai.prompt)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
```

- [ ] **Step 3: Rewrite HalogenAppViewModel to be Nano-only**

Replace the full content of `sample/src/main/kotlin/me/mmckenna/halogen/sample/HalogenAppViewModel.kt`:

```kotlin
package me.mmckenna.halogen.sample

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import halogen.HalogenDefaults
import halogen.HalogenLlmAvailability
import halogen.cache.room.HalogenRoomCache
import halogen.engine.Halogen
import halogen.engine.HalogenEngine
import halogen.provider.nano.GeminiNanoProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HalogenAppViewModel(application: Application) : AndroidViewModel(application) {
    val nanoProvider = GeminiNanoProvider()

    val engine: HalogenEngine = Halogen.Builder()
        .provider(nanoProvider)
        .defaultTheme(HalogenDefaults.materialYou())
        .cache(HalogenRoomCache.create())
        .build()

    private val _nanoStatus = MutableStateFlow(HalogenLlmAvailability.UNAVAILABLE)
    val nanoStatus: StateFlow<HalogenLlmAvailability> = _nanoStatus.asStateFlow()

    private val _darkOverride = MutableStateFlow<Boolean?>(null)
    val darkOverride: StateFlow<Boolean?> = _darkOverride.asStateFlow()

    init {
        viewModelScope.launch {
            while (_nanoStatus.value != HalogenLlmAvailability.READY) {
                try {
                    _nanoStatus.value = nanoProvider.availability()
                } catch (_: Exception) {
                }
                delay(3000)
            }
            Log.d("HalogenDemo", "Gemini Nano is ready")
        }
    }

    fun toggleDarkMode(currentIsDark: Boolean) {
        _darkOverride.value = !currentIsDark
    }

    fun updateTuningParams(
        temperature: Float,
        topK: Int,
        topP: Float?,
    ) {
        nanoProvider.temperature = temperature
        nanoProvider.topK = topK
        nanoProvider.topP = topP
    }
}
```

- [ ] **Step 4: Verify sample compiles**

Run: `./gradlew -q --console=plain :sample:compileDebugKotlin 2>&1 | tail -5`

Expected: Will have compile errors in the screens that still reference removed types. That's expected - we fix those in Task 6.

- [ ] **Step 5: Commit**

```bash
git add -A sample/src/main/kotlin/me/mmckenna/halogen/sample/llms \
        sample/build.gradle.kts \
        sample/src/main/kotlin/me/mmckenna/halogen/sample/HalogenAppViewModel.kt
git commit -m "refactor: remove OpenAI/Retrofit code from Android sample, Nano-only provider"
```

---

### Task 6: Update Android sample screens to remove OpenAI/provider-switcher references

**Files:**
- Modify: `sample/src/main/kotlin/me/mmckenna/halogen/sample/HalogenDemoApp.kt`
- Modify: `sample/src/main/kotlin/me/mmckenna/halogen/sample/screens/settings/SettingsScreen.kt`
- Modify: `sample/src/main/kotlin/me/mmckenna/halogen/sample/screens/playground/PlaygroundScreen.kt`
- Modify: `sample/src/main/kotlin/me/mmckenna/halogen/sample/screens/playground/PlaygroundViewModel.kt`

- [ ] **Step 1: Update HalogenDemoApp to remove provider switcher params**

Replace the full content of `sample/src/main/kotlin/me/mmckenna/halogen/sample/HalogenDemoApp.kt`:

```kotlin
package me.mmckenna.halogen.sample

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import halogen.compose.HalogenTheme
import me.mmckenna.halogen.sample.screens.Screen
import me.mmckenna.halogen.sample.screens.playground.PlaygroundScreen
import me.mmckenna.halogen.sample.screens.settings.SettingsScreen
import me.mmckenna.halogen.sample.screens.testharness.ThemeTestHarnessScreen
import me.mmckenna.halogen.sample.weather.WeatherScreen

private val bottomNavScreens = listOf(Screen.Playground, Screen.Weather, Screen.Test, Screen.Settings)

@Composable
fun HalogenDemoApp() {
    val appViewModel: HalogenAppViewModel = viewModel()

    val nanoStatus by appViewModel.nanoStatus.collectAsState()
    val darkOverride by appViewModel.darkOverride.collectAsState()
    val isDark = darkOverride ?: isSystemInDarkTheme()

    val themeSpec by appViewModel.engine.activeTheme.collectAsState()
    val navController = rememberNavController()

    HalogenTheme(spec = themeSpec, darkTheme = isDark, config = appViewModel.engine.config) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = { appViewModel.toggleDarkMode(isDark) }) {
                    Icon(
                        imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = if (isDark) "Switch to light" else "Switch to dark",
                    )
                }
            },
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                NavigationBar {
                    bottomNavScreens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Playground.route,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(Screen.Playground.route) {
                    PlaygroundScreen(
                        engine = appViewModel.engine,
                        nanoProvider = appViewModel.nanoProvider,
                        nanoStatus = nanoStatus,
                        appViewModel = appViewModel,
                        onNavigateToSettings = {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
                composable(Screen.Weather.route) {
                    WeatherScreen(engine = appViewModel.engine)
                }
                composable(Screen.Test.route) {
                    ThemeTestHarnessScreen(
                        engine = appViewModel.engine,
                        nanoProvider = appViewModel.nanoProvider,
                        isDark = isDark,
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        engine = appViewModel.engine,
                        nanoProvider = appViewModel.nanoProvider,
                        nanoStatus = nanoStatus,
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Update SettingsScreen to remove provider switcher**

Replace the full content of `sample/src/main/kotlin/me/mmckenna/halogen/sample/screens/settings/SettingsScreen.kt`:

```kotlin
package me.mmckenna.halogen.sample.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import halogen.HalogenLlmAvailability
import halogen.compose.HalogenSettingsCard
import halogen.engine.HalogenEngine
import halogen.provider.nano.GeminiNanoProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    engine: HalogenEngine,
    nanoProvider: GeminiNanoProvider,
    nanoStatus: HalogenLlmAvailability,
) {
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Companion.factory(engine, nanoProvider))
    val uiState by viewModel.uiState.collectAsState()
    val themeSpec by engine.activeTheme.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        TopAppBar(title = { Text("Settings") })

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Gemini Nano status card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (nanoStatus) {
                        HalogenLlmAvailability.READY -> MaterialTheme.colorScheme.primaryContainer
                        HalogenLlmAvailability.INITIALIZING -> MaterialTheme.colorScheme.tertiaryContainer
                        HalogenLlmAvailability.UNAVAILABLE -> MaterialTheme.colorScheme.errorContainer
                    },
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Gemini Nano", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = when (nanoStatus) {
                                HalogenLlmAvailability.READY -> "Ready"
                                HalogenLlmAvailability.INITIALIZING -> "Initializing / Downloading..."
                                HalogenLlmAvailability.UNAVAILABLE -> "Unavailable"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        if (nanoStatus == HalogenLlmAvailability.INITIALIZING || uiState.isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                    }

                    Text(
                        text = when (nanoStatus) {
                            HalogenLlmAvailability.READY ->
                                "On-device Gemini Nano is ready. Theme generation will run locally with no network needed."
                            HalogenLlmAvailability.INITIALIZING ->
                                "The model is being downloaded or prepared. This may take a few minutes. " +
                                    "Try generating a theme once the status changes to Ready."
                            HalogenLlmAvailability.UNAVAILABLE ->
                                "Gemini Nano is not available on this device. Requires a supported device " +
                                    "(Pixel 9+, Samsung S25+) with a locked bootloader."
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )

                    if (nanoStatus == HalogenLlmAvailability.INITIALIZING) {
                        Button(
                            onClick = { viewModel.downloadModel() },
                            enabled = !uiState.isDownloading,
                        ) {
                            Text(if (uiState.isDownloading) "Downloading..." else "Download Model")
                        }
                    }

                    if (nanoStatus == HalogenLlmAvailability.READY) {
                        OutlinedButton(
                            onClick = { viewModel.warmupModel() },
                        ) {
                            Text("Warmup Model")
                        }
                    }

                    if (uiState.downloadMessage != null) {
                        Text(
                            uiState.downloadMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Halogen settings card for global theme
            HalogenSettingsCard(
                onGenerate = { prompt -> viewModel.generateGlobalTheme(prompt) },
                isLoading = uiState.isLoading,
                currentSpec = themeSpec,
            )

            // Cache management
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Cache", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()
                    Text(
                        "In-memory LRU cache (20 entries max)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = { viewModel.clearCache() },
                    ) {
                        Text("Clear Cache")
                    }
                    OutlinedButton(
                        onClick = { viewModel.resetToDefault() },
                    ) {
                        Text("Reset to Default Theme")
                    }
                    Button(
                        onClick = { viewModel.clearCacheAndReset() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) {
                        Text("Clear All & Reset")
                    }
                }
            }

            // App info
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("About", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()
                    Text("Halogen Playground", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "A demo app for the Halogen library \u2014 LLM-generated Material 3 theming " +
                            "for Compose Multiplatform. Describe a vibe in natural language and " +
                            "watch your entire UI transform.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Version 1.0",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
```

- [ ] **Step 3: Update PlaygroundViewModel to remove OpenAI tuning sync**

Replace the full content of `sample/src/main/kotlin/me/mmckenna/halogen/sample/screens/playground/PlaygroundViewModel.kt`:

```kotlin
package me.mmckenna.halogen.sample.screens.playground

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import halogen.HalogenConfig
import halogen.HalogenLlmAvailability
import halogen.HalogenThemeSpec
import halogen.engine.HalogenEngine
import halogen.engine.HalogenResult
import halogen.provider.nano.GeminiNanoProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.mmckenna.halogen.sample.HalogenAppViewModel

data class PlaygroundUiState(
    val prompt: String = "",
    val isLoading: Boolean = false,
    val selectedConfigName: String = "Default",
    val history: List<HalogenThemeSpec> = emptyList(),
    val processSteps: List<String> = emptyList(),
)

class PlaygroundViewModel(
    private val engine: HalogenEngine,
    private val appViewModel: HalogenAppViewModel,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaygroundUiState())
    val uiState: StateFlow<PlaygroundUiState> = _uiState.asStateFlow()

    var temperature by mutableFloatStateOf(0.3f)
        private set
    var topK by mutableIntStateOf(10)
        private set
    var topP by mutableFloatStateOf(0.9f)
        private set
    var topPEnabled by mutableStateOf(false)
        private set
    var maxTokens by mutableIntStateOf(300)
        private set
    var showTuningControls by mutableStateOf(false)
        private set

    fun updateTemperature(value: Float) {
        temperature = value
        syncTuningParams()
    }

    fun updateTopK(value: Int) {
        topK = value
        syncTuningParams()
    }

    fun updateTopP(value: Float) {
        topP = value
        syncTuningParams()
    }

    fun updateTopPEnabled(enabled: Boolean) {
        topPEnabled = enabled
        syncTuningParams()
    }

    fun updateMaxTokens(value: Int) {
        maxTokens = value
    }

    fun toggleTuningControls() {
        showTuningControls = !showTuningControls
    }

    private fun syncTuningParams() {
        appViewModel.updateTuningParams(
            temperature = temperature,
            topK = topK,
            topP = if (topPEnabled) topP else null,
        )
    }

    fun setPrompt(text: String) {
        _uiState.update { it.copy(prompt = text) }
    }

    fun setSelectedConfig(name: String) {
        _uiState.update { it.copy(selectedConfigName = name) }
    }

    fun generateTheme(nanoProvider: GeminiNanoProvider) {
        val state = _uiState.value
        if (state.prompt.isBlank() || state.isLoading) return

        val config = HalogenConfig.presets[state.selectedConfigName] ?: HalogenConfig.Default
        val cacheKey = "${state.prompt}:${state.selectedConfigName}"

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, processSteps = listOf()) }

            engine.config = config
            addStep("Prompt: \"${state.prompt}\"")
            addStep("Config: ${state.selectedConfigName}")
            addStep("Checking Nano availability...")

            val availability = try {
                nanoProvider.availability()
            } catch (_: Exception) {
                HalogenLlmAvailability.UNAVAILABLE
            }
            addStep("Nano: $availability")

            addStep("Resolving theme...")
            val startTime = System.currentTimeMillis()
            val result = engine.resolve(key = cacheKey, hint = state.prompt)
            val elapsed = System.currentTimeMillis() - startTime

            when (result) {
                is HalogenResult.Success -> {
                    addStep("LLM generated in ${elapsed}ms")
                    addStep("Seeds: pri=${result.spec.primary} sec=${result.spec.secondary} ter=${result.spec.tertiary}")
                    addStep("Neutral: L=${result.spec.neutralLight} D=${result.spec.neutralDark}")
                    addStep("Style: ${result.spec.fontMood}, ${result.spec.cornerStyle} corners")
                    addStep("Expanded to M3 palette (light + dark)")
                }
                is HalogenResult.Cached -> {
                    addStep("Cache hit (${elapsed}ms)")
                    addStep("Seeds: pri=${result.spec.primary}")
                }
                is HalogenResult.ParseError -> {
                    addStep("Parse error: ${result.message}")
                    result.rawResponse?.take(100)?.let {
                        addStep("Raw: $it")
                    }
                }
                is HalogenResult.Unavailable -> {
                    addStep("All providers unavailable")
                    addStep("Using default M3 theme")
                }
                else -> addStep("Result: ${result::class.simpleName}")
            }

            result.themeSpec?.let { spec ->
                addToHistory(spec)
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun regenerateTheme(hint: String) {
        val state = _uiState.value
        if (hint.isBlank() || state.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            engine.regenerate(key = hint, hint = hint)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun applyFromHistory(spec: HalogenThemeSpec) {
        viewModelScope.launch {
            engine.apply(spec.primary, spec)
        }
    }

    private fun addStep(step: String) {
        _uiState.update { it.copy(processSteps = it.processSteps + step) }
    }

    private fun addToHistory(spec: HalogenThemeSpec) {
        _uiState.update { current ->
            val newHistory = if (current.history.contains(spec)) {
                current.history
            } else {
                (listOf(spec) + current.history).take(10)
            }
            current.copy(history = newHistory)
        }
    }

    companion object {
        fun factory(engine: HalogenEngine, appViewModel: HalogenAppViewModel) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PlaygroundViewModel(engine, appViewModel) as T
            }
        }
    }
}
```

- [ ] **Step 4: Update PlaygroundScreen to remove LlmProviderChoice references**

In `sample/src/main/kotlin/me/mmckenna/halogen/sample/screens/playground/PlaygroundScreen.kt`:

Remove the import:
```kotlin
import me.mmckenna.halogen.sample.llms.LlmProviderChoice
```

Update the function signature - remove the `selectedProvider` parameter:

```kotlin
@Composable
fun PlaygroundScreen(
    engine: HalogenEngine,
    nanoProvider: GeminiNanoProvider,
    nanoStatus: HalogenLlmAvailability,
    appViewModel: HalogenAppViewModel,
    onNavigateToSettings: () -> Unit = {},
) {
```

Replace the provider status indicator in the TopAppBar actions (the entire `Row` block inside `actions`). Replace:

```kotlin
                // Provider status indicator — tap to go to Settings
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onNavigateToSettings() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    val statusColor = when (selectedProvider) {
                        LlmProviderChoice.OPENAI -> Color(0xFF4CAF50)
                        LlmProviderChoice.GEMINI_NANO -> when (nanoStatus) {
                            HalogenLlmAvailability.READY -> Color(0xFF4CAF50)
                            HalogenLlmAvailability.INITIALIZING -> Color(0xFFFFC107)
                            HalogenLlmAvailability.UNAVAILABLE -> Color(0xFFF44336)
                        }
                    }
                    val statusText = when (selectedProvider) {
                        LlmProviderChoice.OPENAI -> "OpenAI"
                        LlmProviderChoice.GEMINI_NANO -> when (nanoStatus) {
                            HalogenLlmAvailability.READY -> "Nano"
                            HalogenLlmAvailability.INITIALIZING -> "Downloading..."
                            HalogenLlmAvailability.UNAVAILABLE -> "Nano Unavailable"
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
```

With:

```kotlin
                // Nano status indicator — tap to go to Settings
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onNavigateToSettings() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    val statusColor = when (nanoStatus) {
                        HalogenLlmAvailability.READY -> Color(0xFF4CAF50)
                        HalogenLlmAvailability.INITIALIZING -> Color(0xFFFFC107)
                        HalogenLlmAvailability.UNAVAILABLE -> Color(0xFFF44336)
                    }
                    val statusText = when (nanoStatus) {
                        HalogenLlmAvailability.READY -> "Nano"
                        HalogenLlmAvailability.INITIALIZING -> "Downloading..."
                        HalogenLlmAvailability.UNAVAILABLE -> "Nano Unavailable"
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
```

- [ ] **Step 5: Verify full build compiles**

Run: `./gradlew -q --console=plain :sample:compileDebugKotlin :sample-shared:compileKotlinJvm 2>&1 | tail -5`

Expected: BUILD SUCCESSFUL for both modules.

- [ ] **Step 6: Commit**

```bash
git add sample/src/main/kotlin/me/mmckenna/halogen/sample/HalogenDemoApp.kt \
       sample/src/main/kotlin/me/mmckenna/halogen/sample/screens/settings/SettingsScreen.kt \
       sample/src/main/kotlin/me/mmckenna/halogen/sample/screens/playground/PlaygroundViewModel.kt \
       sample/src/main/kotlin/me/mmckenna/halogen/sample/screens/playground/PlaygroundScreen.kt
git commit -m "refactor: update Android sample screens to remove provider switcher UI"
```

---

### Task 7: Final build verification

**Files:** None (verification only)

- [ ] **Step 1: Full Gradle build of both modules**

Run: `./gradlew -q --console=plain :sample-shared:compileKotlinJvm :sample-shared:compileKotlinWasmJs :sample:compileDebugKotlin 2>&1 | tail -10`

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run JVM Desktop app smoke test**

Run: `./gradlew -q --console=plain :sample-shared:jvmRun 2>&1 | head -5` (will open a window - cancel after verifying it launches)

Expected: Window opens with Halogen Playground title. If `OPENAI_API_KEY` is set, OpenAI provider is active; otherwise DemoProvider.

- [ ] **Step 3: Commit plan and spec together**

```bash
git add docs/superpowers/specs/2026-03-27-kmp-openai-provider-design.md \
       docs/superpowers/plans/2026-03-27-kmp-openai-provider.md
git commit -m "docs: add KMP OpenAI provider design spec and implementation plan"
```
