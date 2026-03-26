package me.mmckenna.halogen.sample.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(val label: String, val icon: ImageVector) {
    Playground("Playground", Icons.Default.PlayArrow),
    Weather("Weather", Icons.Default.Cloud),
    Test("Test", Icons.Default.Science),
    Settings("Settings", Icons.Default.Settings),
}
