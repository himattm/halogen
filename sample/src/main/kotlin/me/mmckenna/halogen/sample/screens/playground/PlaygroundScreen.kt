package me.mmckenna.halogen.sample.screens.playground

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import halogen.HalogenConfig
import halogen.HalogenLlmAvailability
import halogen.ThemeExpander
import halogen.engine.HalogenEngine
import halogen.provider.nano.GeminiNanoProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.mmckenna.halogen.sample.llms.LlmProviderChoice
import me.mmckenna.halogen.sample.screens.ThemeInspectorSheet

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlaygroundScreen(
    engine: HalogenEngine,
    nanoProvider: GeminiNanoProvider,
    nanoStatus: HalogenLlmAvailability,
    selectedProvider: LlmProviderChoice = LlmProviderChoice.GEMINI_NANO,
    onNavigateToSettings: () -> Unit = {},
) {
    val viewModel: PlaygroundViewModel = viewModel(factory = PlaygroundViewModel.Companion.factory(engine))
    val uiState by viewModel.uiState.collectAsState()

    var showInspector by remember { mutableStateOf(false) }
    val themeSpec by engine.activeTheme.collectAsState()
    val isDark = isSystemInDarkTheme()

    var showProcess by remember { mutableStateOf(false) }
    var showConfigMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        TopAppBar(
            title = { Text("Playground") },
            actions = {
                // Provider status indicator — tap to go to Settings
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onNavigateToSettings() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    val statusColor = when (selectedProvider) {
                        LlmProviderChoice.OPENAI -> Color(0xFF4CAF50)
                        LlmProviderChoice.GEMINI_NANO -> when (nanoStatus) {
                            HalogenLlmAvailability.READY -> Color(0xFF4CAF50)
                            HalogenLlmAvailability.INITIALIZING -> Color(0xFFFFC107)
                            HalogenLlmAvailability.UNAVAILABLE -> Color(0xFFF44336)
                        }
                    }
                    val statusText = when (selectedProvider) {
                        LlmProviderChoice.OPENAI -> "OpenAI"
                        LlmProviderChoice.GEMINI_NANO -> when (nanoStatus) {
                            HalogenLlmAvailability.READY -> "Nano"
                            HalogenLlmAvailability.INITIALIZING -> "Downloading..."
                            HalogenLlmAvailability.UNAVAILABLE -> "Nano Unavailable"
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (themeSpec != null) {
                    IconButton(onClick = { showInspector = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Inspect theme")
                    }
                }
            },
        )

        // Status banner when not ready
        if (nanoStatus != HalogenLlmAvailability.READY) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        when (nanoStatus) {
                            HalogenLlmAvailability.INITIALIZING -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.errorContainer
                        },
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (nanoStatus == HalogenLlmAvailability.INITIALIZING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
                Text(
                    text = when (nanoStatus) {
                        HalogenLlmAvailability.INITIALIZING ->
                            "Gemini Nano is downloading. Theme generation will work once ready."
                        HalogenLlmAvailability.UNAVAILABLE ->
                            "Gemini Nano is unavailable on this device. Requires Pixel 9+ or Samsung S25+."
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when (nanoStatus) {
                        HalogenLlmAvailability.INITIALIZING -> MaterialTheme.colorScheme.onTertiaryContainer
                        else -> MaterialTheme.colorScheme.onErrorContainer
                    },
                )
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Prompt input
            OutlinedTextField(
                value = uiState.prompt,
                onValueChange = { viewModel.setPrompt(it) },
                label = { Text("Describe a vibe...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3,
                enabled = !uiState.isLoading,
            )

            // Config preset picker
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Style:", style = MaterialTheme.typography.labelMedium)
                Box {
                    TextButton(onClick = { showConfigMenu = true }) {
                        Text(uiState.selectedConfigName)
                    }
                    DropdownMenu(
                        expanded = showConfigMenu,
                        onDismissRequest = { showConfigMenu = false },
                    ) {
                        HalogenConfig.presets.keys.forEach { name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    viewModel.setSelectedConfig(name)
                                    showConfigMenu = false
                                },
                            )
                        }
                    }
                }
            }

            // Action row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = {
                        showProcess = true
                        viewModel.generateTheme(nanoProvider)
                    },
                    enabled = uiState.prompt.isNotBlank() && !uiState.isLoading,
                ) {
                    Text("Generate")
                }

                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }

            // Process log — expandable card showing Nano's thinking
            if (uiState.processSteps.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showProcess = !showProcess },
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 1.5.dp,
                                    )
                                }
                                Text(
                                    text = if (uiState.isLoading) "Generating..." else uiState.processSteps.lastOrNull() ?: "",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Text(
                                text = if (showProcess) "Hide" else "Show",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (showProcess) {
                            Spacer(modifier = Modifier.height(8.dp))
                            uiState.processSteps.forEach { step ->
                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 1.dp),
                                )
                            }
                        }
                    }
                }
            }

            // History swatches
            if (uiState.history.isNotEmpty()) {
                Text("Recent", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    uiState.history.take(8).forEach { spec ->
                        val primaryColor by produceState(Color.Transparent, spec, isDark) {
                            value = withContext(Dispatchers.Default) {
                                Color(ThemeExpander.expandColors(spec, isDark = isDark).primary)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(primaryColor)
                                .clickable {
                                    viewModel.applyFromHistory(spec)
                                },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Component showcase
            Text("Component Showcase", style = MaterialTheme.typography.titleMedium)

            // Buttons row
            Text("Buttons", style = MaterialTheme.typography.labelMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = {}) { Text("Filled") }
                OutlinedButton(onClick = {}) { Text("Outlined") }
                ElevatedButton(onClick = {}) { Text("Elevated") }
                FilledTonalButton(onClick = {}) { Text("Tonal") }
            }

            // FAB
            Text("FAB", style = MaterialTheme.typography.labelMedium)
            FloatingActionButton(onClick = {}) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }

            // Card
            Text("Card", style = MaterialTheme.typography.labelMedium)
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Card Title", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "This is body text inside a Material 3 card. It demonstrates " +
                            "how surface and content colors are applied by the current theme.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            // TextField
            Text("TextField", style = MaterialTheme.typography.labelMedium)
            var sampleText by remember { mutableStateOf("Sample input") }
            OutlinedTextField(
                value = sampleText,
                onValueChange = { sampleText = it },
                label = { Text("Label") },
                modifier = Modifier.fillMaxWidth(),
            )

            // Switch
            Text("Switch", style = MaterialTheme.typography.labelMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                var checked by remember { mutableStateOf(true) }
                Switch(checked = checked, onCheckedChange = { checked = it })
                Text(if (checked) "On" else "Off")

                var unchecked by remember { mutableStateOf(false) }
                Switch(checked = unchecked, onCheckedChange = { unchecked = it })
                Text(if (unchecked) "On" else "Off")
            }

            // Slider
            Text("Slider", style = MaterialTheme.typography.labelMedium)
            var sliderValue by remember { mutableFloatStateOf(0.5f) }
            Slider(value = sliderValue, onValueChange = { sliderValue = it })

            // FilterChip
            Text("Chips", style = MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Design", "Code", "Music", "Art").forEach { label ->
                    var selected by remember { mutableStateOf(false) }
                    FilterChip(
                        selected = selected,
                        onClick = { selected = !selected },
                        label = { Text(label) },
                    )
                }
            }

            // NavigationBar preview strip
            Text("NavigationBar", style = MaterialTheme.typography.labelMedium)
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home") },
                    selected = true,
                    onClick = {},
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                    label = { Text("Play") },
                    selected = false,
                    onClick = {},
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {},
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showInspector) {
        ThemeInspectorSheet(
            spec = themeSpec,
            onDismiss = { showInspector = false },
            onRegenerate = {
                if (uiState.prompt.isNotBlank()) {
                    showInspector = false
                    viewModel.regenerateTheme(uiState.prompt)
                }
            },
        )
    }
}
