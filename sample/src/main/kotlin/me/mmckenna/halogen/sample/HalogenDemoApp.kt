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
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import halogen.compose.HalogenTheme
import me.mmckenna.halogen.sample.screens.Screen
import me.mmckenna.halogen.sample.screens.playground.PlaygroundScreen
import me.mmckenna.halogen.sample.screens.settings.SettingsScreen
import me.mmckenna.halogen.sample.screens.testharness.ThemeTestHarnessScreen
import me.mmckenna.halogen.sample.weather.WeatherScreen

private val bottomNavScreens = listOf(Screen.Playground, Screen.Weather, Screen.Test, Screen.Settings)

@Composable
fun HalogenDemoApp() {
    val appViewModel: HalogenAppViewModel = viewModel()

    val selectedProvider by appViewModel.selectedProvider.collectAsState()
    val nanoStatus by appViewModel.nanoStatus.collectAsState()
    val darkOverride by appViewModel.darkOverride.collectAsState()
    val isDark = darkOverride ?: isSystemInDarkTheme()

    val themeSpec by appViewModel.engine.activeTheme.collectAsState()
    val navController = rememberNavController()

    HalogenTheme(spec = themeSpec, darkTheme = isDark, config = appViewModel.engine.config) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = { appViewModel.toggleDarkMode(isDark) }) {
                    Icon(
                        imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = if (isDark) "Switch to light" else "Switch to dark",
                    )
                }
            },
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                NavigationBar {
                    bottomNavScreens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Playground.route,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(Screen.Playground.route) {
                  PlaygroundScreen(
                    engine = appViewModel.engine,
                    nanoProvider = appViewModel.nanoProvider,
                    nanoStatus = nanoStatus,
                    selectedProvider = selectedProvider,
                    onNavigateToSettings = {
                      navController.navigate(Screen.Settings.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                      }
                    },
                  )
                }
                composable(Screen.Weather.route) {
                  WeatherScreen(engine = appViewModel.engine)
                }
                composable(Screen.Test.route) {
                    ThemeTestHarnessScreen(
                        engine = appViewModel.engine,
                        nanoProvider = appViewModel.nanoProvider,
                        isDark = isDark,
                    )
                }
                composable(Screen.Settings.route) {
                  SettingsScreen(
                    engine = appViewModel.engine,
                    nanoProvider = appViewModel.nanoProvider,
                    nanoStatus = nanoStatus,
                    selectedProvider = selectedProvider,
                    openAiAvailable = appViewModel.openAiAvailable,
                    onProviderChanged = { appViewModel.setSelectedProvider(it) },
                  )
                }
            }
        }
    }
}
