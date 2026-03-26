package me.mmckenna.halogen.sample.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.lifecycle.viewmodel.compose.viewModel
import halogen.HalogenLlmAvailability
import halogen.compose.HalogenSettingsCard
import halogen.engine.HalogenEngine
import halogen.provider.nano.GeminiNanoProvider
import me.mmckenna.halogen.sample.llms.LlmProviderChoice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    engine: HalogenEngine,
    nanoProvider: GeminiNanoProvider,
    nanoStatus: HalogenLlmAvailability,
    selectedProvider: LlmProviderChoice = LlmProviderChoice.GEMINI_NANO,
    openAiAvailable: Boolean = false,
    onProviderChanged: (LlmProviderChoice) -> Unit = {},
) {
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Companion.factory(engine, nanoProvider))
    val uiState by viewModel.uiState.collectAsState()
    val themeSpec by engine.activeTheme.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        TopAppBar(title = { Text("Settings") })

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // LLM Provider selector
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("LLM Provider", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()
                    Text(
                        "Choose which model generates themes. Nano runs on-device; OpenAI requires network.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    @OptIn(ExperimentalMaterial3Api::class)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        LlmProviderChoice.entries.forEachIndexed { index, choice ->
                            val enabled = when (choice) {
                                LlmProviderChoice.OPENAI -> openAiAvailable
                                LlmProviderChoice.GEMINI_NANO -> true
                            }
                            SegmentedButton(
                                selected = selectedProvider == choice,
                                onClick = { if (enabled) onProviderChanged(choice) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = LlmProviderChoice.entries.size,
                                ),
                                enabled = enabled,
                            ) {
                                Text(choice.label)
                            }
                        }
                    }
                    if (selectedProvider == LlmProviderChoice.OPENAI && !openAiAvailable) {
                        Text(
                            "Set OPENAI_API_KEY in local.properties to enable.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            // Gemini Nano status card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (nanoStatus) {
                        HalogenLlmAvailability.READY -> MaterialTheme.colorScheme.primaryContainer
                        HalogenLlmAvailability.INITIALIZING -> MaterialTheme.colorScheme.tertiaryContainer
                        HalogenLlmAvailability.UNAVAILABLE -> MaterialTheme.colorScheme.errorContainer
                    },
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Gemini Nano", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = when (nanoStatus) {
                                HalogenLlmAvailability.READY -> "Ready"
                                HalogenLlmAvailability.INITIALIZING -> "Initializing / Downloading..."
                                HalogenLlmAvailability.UNAVAILABLE -> "Unavailable"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        if (nanoStatus == HalogenLlmAvailability.INITIALIZING || uiState.isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                    }

                    Text(
                        text = when (nanoStatus) {
                            HalogenLlmAvailability.READY ->
                                "On-device Gemini Nano is ready. Theme generation will run locally with no network needed."
                            HalogenLlmAvailability.INITIALIZING ->
                                "The model is being downloaded or prepared. This may take a few minutes. " +
                                    "Try generating a theme once the status changes to Ready."
                            HalogenLlmAvailability.UNAVAILABLE ->
                                "Gemini Nano is not available on this device. Requires a supported device " +
                                    "(Pixel 9+, Samsung S25+) with a locked bootloader."
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )

                    // Download / warmup button
                    if (nanoStatus == HalogenLlmAvailability.INITIALIZING) {
                        Button(
                            onClick = { viewModel.downloadModel() },
                            enabled = !uiState.isDownloading,
                        ) {
                            Text(if (uiState.isDownloading) "Downloading..." else "Download Model")
                        }
                    }

                    if (nanoStatus == HalogenLlmAvailability.READY) {
                        OutlinedButton(
                            onClick = { viewModel.warmupModel() },
                        ) {
                            Text("Warmup Model")
                        }
                    }

                    if (uiState.downloadMessage != null) {
                        Text(
                            uiState.downloadMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Halogen settings card for global theme
            HalogenSettingsCard(
                onGenerate = { prompt -> viewModel.generateGlobalTheme(prompt) },
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
                        onClick = { viewModel.clearCache() },
                    ) {
                        Text("Clear Cache")
                    }
                    OutlinedButton(
                        onClick = { viewModel.resetToDefault() },
                    ) {
                        Text("Reset to Default Theme")
                    }
                    Button(
                        onClick = { viewModel.clearCacheAndReset() },
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
                    Text("Halogen Demo", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "A demo app for the Halogen library — LLM-generated Material 3 theming " +
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
