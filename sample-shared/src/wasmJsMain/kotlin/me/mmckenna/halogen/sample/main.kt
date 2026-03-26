package me.mmckenna.halogen.sample

import androidx.compose.runtime.remember
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
        val demoState = remember { HalogenDemoState.create(scope) }
        HalogenDemoApp(demoState = demoState)
    }
}
