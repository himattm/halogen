package me.mmckenna.halogen.sample.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Playground : Screen("playground", "Playground", Icons.Default.PlayArrow)
    data object Weather : Screen("weather", "Weather", Icons.Default.Cloud)
    data object Test : Screen("test", "Test", Icons.Default.Science)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}
