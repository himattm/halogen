package me.mmckenna.halogen.sample

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun MainViewController() = ComposeUIViewController {
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    val apiKey = remember { getEnvVar("OPENAI_API_KEY") }
    val demoState = remember { HalogenDemoState.create(scope, openAiApiKey = apiKey) }

    HalogenDemoApp(demoState = demoState)
}
