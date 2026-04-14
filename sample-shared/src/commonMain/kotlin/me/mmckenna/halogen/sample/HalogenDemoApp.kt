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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import halogen.compose.HalogenTheme
import me.mmckenna.halogen.sample.screens.Screen
import me.mmckenna.halogen.sample.screens.image.ImageScreen
import me.mmckenna.halogen.sample.screens.image.ImageState
import me.mmckenna.halogen.sample.screens.playground.PlaygroundScreen
import me.mmckenna.halogen.sample.screens.playground.PlaygroundState
import me.mmckenna.halogen.sample.screens.settings.SettingsScreen
import me.mmckenna.halogen.sample.screens.settings.SettingsState
import me.mmckenna.halogen.sample.screens.testharness.TestHarnessState
import me.mmckenna.halogen.sample.screens.testharness.ThemeTestHarnessScreen
import me.mmckenna.halogen.sample.weather.WeatherScreen
import me.mmckenna.halogen.sample.weather.WeatherState

private val bottomNavScreens = listOf(Screen.Playground, Screen.Weather, Screen.Image, Screen.Test, Screen.Settings)

@Composable
fun HalogenDemoApp(demoState: HalogenDemoState) {
    val scope = rememberCoroutineScope()
    val engine = demoState.engine

    val darkOverride by demoState.darkOverride.collectAsState()
    val isDark = darkOverride ?: isSystemInDarkTheme()
    val themeSpec by engine.activeTheme.collectAsState()

    var currentScreen by remember { mutableStateOf(Screen.Playground) }

    // Create state objects scoped to the composition
    val playgroundState = remember { PlaygroundState(engine, scope) }
    val weatherState = remember { WeatherState(engine, scope) }
    val testHarnessState = remember { TestHarnessState(engine, scope) }
    val settingsState = remember { SettingsState(engine, scope) }
    val imageState = remember { ImageState(engine, scope) }

    HalogenTheme(spec = themeSpec, darkTheme = isDark, config = engine.config) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = { demoState.toggleDarkMode(isDark) }) {
                    Icon(
                        imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = if (isDark) "Switch to light" else "Switch to dark",
                    )
                }
            },
            bottomBar = {
                NavigationBar {
                    bottomNavScreens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentScreen == screen,
                            onClick = { currentScreen = screen },
                        )
                    }
                }
            },
        ) { innerPadding ->
            when (currentScreen) {
                Screen.Playground -> PlaygroundScreen(
                    state = playgroundState,
                    engine = engine,
                    isDark = isDark,
                    providerName = demoState.providerName,
                    onNavigateToSettings = { currentScreen = Screen.Settings },
                    modifier = Modifier.padding(innerPadding),
                )
                Screen.Weather -> WeatherScreen(
                    state = weatherState,
                    engine = engine,
                    modifier = Modifier.padding(innerPadding),
                )
                Screen.Image -> ImageScreen(
                    state = imageState,
                    engine = engine,
                    isDark = isDark,
                    modifier = Modifier.padding(innerPadding),
                )
                Screen.Test -> ThemeTestHarnessScreen(
                    state = testHarnessState,
                    isDark = isDark,
                    modifier = Modifier.padding(innerPadding),
                )
                Screen.Settings -> SettingsScreen(
                    state = settingsState,
                    engine = engine,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}
