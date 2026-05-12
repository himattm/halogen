package me.mmckenna.halogen.sample

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import halogen.provider.apple.foundation.AppleFoundationBridge
import halogen.provider.apple.foundation.AppleFoundationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun MainViewController(appleFoundationBridge: AppleFoundationBridge? = null) =
    ComposeUIViewController {
        val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
        val apiKey = remember { getEnvVar("OPENAI_API_KEY") }
        val demoState = remember {
            val appleProvider = appleFoundationBridge?.let { AppleFoundationProvider(it) }
            if (appleProvider != null) {
                HalogenDemoState.create(
                    scope = scope,
                    preferredProvider = appleProvider,
                    preferredProviderName = "Apple Foundation Models",
                )
            } else {
                HalogenDemoState.create(scope = scope, openAiApiKey = apiKey)
            }
        }

        HalogenDemoApp(demoState = demoState)
    }
