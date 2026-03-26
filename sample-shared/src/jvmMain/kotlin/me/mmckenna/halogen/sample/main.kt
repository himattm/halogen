package me.mmckenna.halogen.sample

import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import java.util.Properties

fun main() = application {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val apiKey = loadLocalProperty("OPENAI_API_KEY")

    Window(
        onCloseRequest = ::exitApplication,
        title = "Halogen Playground",
    ) {
        val demoState = remember { HalogenDemoState.create(scope, openAiApiKey = apiKey) }
        HalogenDemoApp(demoState = demoState)
    }
}

private fun loadLocalProperty(key: String): String? {
    val file = File("local.properties")
    if (!file.exists()) return null
    val props = Properties()
    file.inputStream().use { props.load(it) }
    return props.getProperty(key)?.takeIf { it.isNotBlank() }
}
