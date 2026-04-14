package me.mmckenna.halogen.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val scope = rememberCoroutineScope()
            val demoState = remember {
                val apiKey = BuildConfig.OPENAI_API_KEY.ifBlank { null }
                HalogenDemoState.create(scope, openAiApiKey = apiKey)
            }
            HalogenDemoApp(demoState)
        }
    }
}
