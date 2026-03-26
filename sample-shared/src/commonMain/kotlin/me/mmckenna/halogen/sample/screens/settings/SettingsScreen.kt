package me.mmckenna.halogen.sample.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import halogen.compose.HalogenSettingsCard
import halogen.engine.HalogenEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    engine: HalogenEngine,
    modifier: Modifier = Modifier,
) {
    val uiState by state.uiState.collectAsState()
    val themeSpec by engine.activeTheme.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        TopAppBar(title = { Text("Settings") })

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Halogen settings card for global theme
            HalogenSettingsCard(
                onGenerate = { prompt -> state.generateGlobalTheme(prompt) },
                isLoading = uiState.isLoading,
                currentSpec = themeSpec,
            )

            // Cache management
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Cache", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()
                    Text(
                        "In-memory LRU cache (20 entries max)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = { state.clearCache() },
                    ) {
                        Text("Clear Cache")
                    }
                    OutlinedButton(
                        onClick = { state.resetToDefault() },
                    ) {
                        Text("Reset to Default Theme")
                    }
                    Button(
                        onClick = { state.clearCacheAndReset() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) {
                        Text("Clear All & Reset")
                    }
                }
            }

            // App info
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("About", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()
                    Text("Halogen Playground", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "A demo app for the Halogen library \u2014 LLM-generated Material 3 theming " +
                            "for Compose Multiplatform. Describe a vibe in natural language and " +
                            "watch your entire UI transform.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Version 1.0",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
