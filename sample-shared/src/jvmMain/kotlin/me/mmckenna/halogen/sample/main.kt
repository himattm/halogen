package me.mmckenna.halogen.sample

import androidx.compose.runtime.remember
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
        val demoState = remember { HalogenDemoState.create(scope, openAiApiKey = apiKey) }
        HalogenDemoApp(demoState = demoState)
    }
}
