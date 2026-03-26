package me.mmckenna.halogen.sample.screens.testharness

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import halogen.HalogenColorScheme
import halogen.HalogenConfig
import halogen.HalogenThemeSpec
import halogen.ThemeExpander
import halogen.engine.HalogenEngine
import halogen.provider.nano.GeminiNanoProvider

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ThemeTestHarnessScreen(
    engine: HalogenEngine,
    nanoProvider: GeminiNanoProvider,
    isDark: Boolean,
) {
    val viewModel: TestHarnessViewModel = viewModel(factory = TestHarnessViewModel.factory(engine))
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        TopAppBar(
            title = { Text("Theme Test Harness") },
        )

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // --- Controls ---
            Text("Config Presets", style = MaterialTheme.typography.labelMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                viewModel.allConfigNames.forEach { name ->
                    FilterChip(
                        selected = uiState.enabledConfigs[name] == true,
                        onClick = { viewModel.toggleConfig(name) },
                        label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }

            // Prompt selector for Run Selected
            Text("Selected Prompt", style = MaterialTheme.typography.labelMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                testPrompts.forEach { prompt ->
                    FilterChip(
                        selected = uiState.selectedPrompt == prompt,
                        onClick = { viewModel.selectPrompt(prompt) },
                        label = { Text(prompt, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Run All: each prompt x each enabled config = separate LLM call
                // So Nano sees "vibrant sun", "pastel sun", etc. as different prompts
                Button(
                    onClick = { viewModel.runAll() },
                    enabled = !uiState.isRunning,
                ) {
                    Text("Run All")
                }

                // Run Selected: one prompt x all enabled configs
                OutlinedButton(
                    onClick = { viewModel.runSelected() },
                    enabled = !uiState.isRunning,
                ) {
                    Text("Run Selected")
                }

                if (uiState.isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }

            // Progress
            if (uiState.isRunning || uiState.progressTotal > 0) {
                Column {
                    Text(
                        text = "${uiState.statusMessage} (${uiState.progressCurrent} / ${uiState.progressTotal})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (uiState.isRunning) {
                        LinearProgressIndicator(
                            progress = { if (uiState.progressTotal > 0) uiState.progressCurrent.toFloat() / uiState.progressTotal else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                        )
                    }
                }
            }

            // Errors
            if (uiState.errors.isNotEmpty()) {
                Text("Errors", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                uiState.errors.forEach { (prompt, error) ->
                    Text(
                        text = "$prompt: $error",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            HorizontalDivider()

            // --- Matrix View ---
            Text("Results Matrix", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Each row = one prompt (one LLM call). Each column = config preset expansion.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (uiState.specMap.isEmpty()) {
                Text(
                    text = "No results yet. Press \"Run All\" or \"Run Selected\" to generate themes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                val activeConfigs = viewModel.allConfigNames.filter { uiState.enabledConfigs[it] == true }

                // Horizontally scrollable matrix
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    // Row header column
                    Column {
                        // Empty corner cell for header alignment
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .height(32.dp),
                        )
                        testPrompts.forEach { prompt ->
                            // Check if any config has a result for this prompt
                            val hasAny = activeConfigs.any { uiState.specMap.containsKey("$prompt:$it") }
                            Box(
                                modifier = Modifier
                                    .width(90.dp)
                                    .height(if (hasAny) 130.dp else 40.dp)
                                    .padding(vertical = 2.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                Text(
                                    text = prompt,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    // Config columns
                    activeConfigs.forEach { configName ->
                        val config = HalogenConfig.presets[configName] ?: HalogenConfig.Default
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            // Column header
                            Box(
                                modifier = Modifier
                                    .width(68.dp)
                                    .height(32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = configName,
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 9.sp,
                                )
                            }

                            // Data cells — each cell has its own LLM-generated spec
                            testPrompts.forEach { prompt ->
                                val cellKey = "$prompt:$configName"
                                val spec = uiState.specMap[cellKey]
                                if (spec != null) {
                                    SwatchCell(spec = spec, config = config, showDark = isDark)
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .width(68.dp)
                                            .height(40.dp)
                                            .padding(2.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = if (uiState.errors.containsKey(cellKey)) "err" else "--",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (uiState.errors.containsKey(cellKey)) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // room for FAB
        }
    }
}

/**
 * A single cell in the matrix showing 3 color swatches for light and dark modes.
 * Light: primary (tone 40), primaryContainer (tone 90), surface (tone 98)
 * Dark: same roles at dark tones.
 */
@Composable
private fun SwatchCell(
    spec: HalogenThemeSpec,
    config: HalogenConfig,
    showDark: Boolean,
) {
    val lightScheme = remember(spec, config) {
        ThemeExpander.expandColors(spec, isDark = false, config = config)
    }
    val darkScheme = remember(spec, config) {
        ThemeExpander.expandColors(spec, isDark = true, config = config)
    }

    Column(
        modifier = Modifier
            .width(68.dp)
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text("L", style = MaterialTheme.typography.labelSmall, fontSize = 7.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SwatchRow(scheme = lightScheme)
        Text(
            text = ThemeExpander.argbToHex(lightScheme.primary),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 7.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        Text("D", style = MaterialTheme.typography.labelSmall, fontSize = 7.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SwatchRow(scheme = darkScheme)
        Text(
            text = ThemeExpander.argbToHex(darkScheme.primary),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 7.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A row of 3 color circles: primary, primaryContainer, surface.
 */
@Composable
private fun SwatchRow(scheme: HalogenColorScheme) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColorCircle(argb = scheme.primary, size = 16)
        ColorCircle(argb = scheme.primaryContainer, size = 16)
        ColorCircle(argb = scheme.surface, size = 16)
    }
}

/**
 * A small colored circle swatch.
 */
@Composable
private fun ColorCircle(argb: Int, size: Int = 16) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color(argb))
            .border(
                width = 0.5.dp,
                color = Color(0x33000000),
                shape = CircleShape,
            ),
    )
}
