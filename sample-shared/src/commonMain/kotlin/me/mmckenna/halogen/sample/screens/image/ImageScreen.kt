package me.mmckenna.halogen.sample.screens.image

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory
import halogen.ThemeExpander
import halogen.engine.HalogenEngine
import halogen.image.DominantColors
import halogen.image.QuantizedColor
import halogen.compose.HalogenTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ImageScreen(
    state: ImageState,
    engine: HalogenEngine,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val uiState by state.uiState.collectAsState()
    val context = LocalPlatformContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .build()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        TopAppBar(
            title = { Text("Image") },
        )

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Subtitle
            Text(
                text = "Tap an image to extract its palette",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Preset image picker — horizontal scroll row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                imagePresets.forEach { preset ->
                    PresetCard(
                        preset = preset,
                        isSelected = uiState.selectedPreset == preset,
                        imageLoader = imageLoader,
                        onClick = { state.selectPreset(preset, imageLoader, context) },
                    )
                }
            }

            // Loading indicator
            if (uiState.isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(
                        "Extracting palette...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Error text
            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            // Palette section
            val colors = uiState.dominantColors
            if (colors != null && colors.colors.isNotEmpty()) {
                PaletteSection(colors = colors)

                HorizontalDivider()

                // LLM toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("Use LLM", style = MaterialTheme.typography.labelLarge)
                        Text(
                            if (uiState.useLlm) "Theme via LLM interpretation" else "Theme via algorithm",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = uiState.useLlm,
                        onCheckedChange = { state.toggleLlm(imageLoader, context) },
                        enabled = !uiState.isLoading,
                    )
                }

                HorizontalDivider()

                // Component showcase wrapped in the extracted theme
                val themeSpec = uiState.themeSpec
                HalogenTheme(spec = themeSpec, darkTheme = isDark) {
                    ComponentShowcase()
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetCard(
    preset: PresetImage,
    isSelected: Boolean,
    imageLoader: ImageLoader,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.size(width = 110.dp, height = 90.dp),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) {
            BorderStroke(2.dp, Color(0xFF6200EE))
        } else {
            null
        },
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = preset.url,
                contentDescription = preset.label,
                imageLoader = imageLoader,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Label overlay at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color(0xAA000000))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            ) {
                Text(
                    text = preset.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun PaletteSection(colors: DominantColors) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Extracted Palette", style = MaterialTheme.typography.titleSmall)

        // Mood descriptor
        val mood = describeMood(colors)
        Text(
            text = "Mood: $mood",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Color swatches row with hex labels
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            colors.colors.forEach { color ->
                ColorSwatch(color = color)
            }
        }
    }
}

@Composable
private fun ColorSwatch(color: QuantizedColor) {
    val hex = remember(color.argb) { ThemeExpander.argbToHex(color.argb) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(color.argb)),
        )
        Text(
            text = hex,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ComponentShowcase() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Themed Components", style = MaterialTheme.typography.titleMedium)

        // Buttons
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
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Card Title", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "This card demonstrates surface and content colors from the extracted image palette.",
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

        // Filter chips
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
    }
}

/**
 * Computes a mood descriptor from the dominant colors based on average tone and chroma.
 *
 * Tone buckets: < 40 → "dark", > 60 → "light", else "mid-tone"
 * Chroma buckets: < 20 → "subdued", < 40 → "moderate", else "vibrant"
 */
private fun describeMood(colors: DominantColors): String {
    if (colors.colors.isEmpty()) return "Neutral"

    val totalPop = colors.colors.sumOf { it.population }
    val avgTone = colors.colors.sumOf { it.tone * it.population } / totalPop
    val avgChroma = colors.colors.sumOf { it.chroma * it.population } / totalPop

    val toneDesc = when {
        avgTone < 40.0 -> "Dark"
        avgTone > 60.0 -> "Light"
        else -> "Mid-tone"
    }

    val chromaDesc = when {
        avgChroma < 20.0 -> "subdued"
        avgChroma < 40.0 -> "moderate"
        else -> "vibrant"
    }

    return "$toneDesc, $chromaDesc"
}
