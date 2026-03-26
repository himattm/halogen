package me.mmckenna.halogen.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import halogen.cache.room.HalogenRoomCache
import halogen.cache.room.initialize

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HalogenRoomCache.initialize(this)
        enableEdgeToEdge()
        setContent {
            HalogenDemoApp()
        }
    }
}
