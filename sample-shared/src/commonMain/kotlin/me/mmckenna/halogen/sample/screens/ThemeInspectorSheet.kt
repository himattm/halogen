package me.mmckenna.halogen.sample.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import halogen.HalogenThemeSpec
import halogen.ThemeExpander

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ThemeInspectorSheet(
    spec: HalogenThemeSpec?,
    onDismiss: () -> Unit,
    onRegenerate: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Theme Inspector", style = MaterialTheme.typography.titleLarge)

            if (spec == null) {
                Text("No theme active", style = MaterialTheme.typography.bodyMedium)
            } else {
                // Light color grid
                Text("Light Colors", style = MaterialTheme.typography.titleSmall)
                ColorGrid(spec = spec, isDark = false)

                HorizontalDivider()

                // Dark color grid
                Text("Dark Colors", style = MaterialTheme.typography.titleSmall)
                ColorGrid(spec = spec, isDark = true)

                HorizontalDivider()

                // Typography preview
                Text("Typography", style = MaterialTheme.typography.titleSmall)
                TypographyPreview(spec = spec)

                HorizontalDivider()

                // Shape preview
                Text("Shapes", style = MaterialTheme.typography.titleSmall)
                ShapePreview(spec = spec)

                HorizontalDivider()

                // Regenerate button
                Button(
                    onClick = onRegenerate,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Regenerate")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorGrid(spec: HalogenThemeSpec, isDark: Boolean) {
    val scheme = remember(spec, isDark) { ThemeExpander.expandColors(spec, isDark = isDark) }
    val colorRoles = remember(scheme) {
        listOf(
            "Primary" to scheme.primary,
            "On Primary" to scheme.onPrimary,
            "Primary Container" to scheme.primaryContainer,
            "On Primary Container" to scheme.onPrimaryContainer,
            "Secondary" to scheme.secondary,
            "On Secondary" to scheme.onSecondary,
            "Secondary Container" to scheme.secondaryContainer,
            "On Secondary Container" to scheme.onSecondaryContainer,
            "Tertiary" to scheme.tertiary,
            "On Tertiary" to scheme.onTertiary,
            "Tertiary Container" to scheme.tertiaryContainer,
            "On Tertiary Container" to scheme.onTertiaryContainer,
            "Error" to scheme.error,
            "On Error" to scheme.onError,
            "Error Container" to scheme.errorContainer,
            "On Error Container" to scheme.onErrorContainer,
            "Surface" to scheme.surface,
            "On Surface" to scheme.onSurface,
            "Surface Variant" to scheme.surfaceVariant,
            "On Surface Variant" to scheme.onSurfaceVariant,
            "Outline" to scheme.outline,
            "Outline Variant" to scheme.outlineVariant,
        )
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        colorRoles.forEach { (name, argb) ->
            ColorSwatch(name = name, argb = argb)
        }
    }
}

@Composable
private fun ColorSwatch(name: String, argb: Int) {
    val hex = remember(argb) {
        ThemeExpander.argbToHex(argb)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(argb)),
        )
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
        Text(
            text = hex,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun TypographyPreview(spec: HalogenThemeSpec) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Font mood: ", style = MaterialTheme.typography.labelMedium)
            Text(spec.fontMood, style = MaterialTheme.typography.bodyMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Heading weight: ", style = MaterialTheme.typography.labelMedium)
            Text("${spec.headingWeight}", style = MaterialTheme.typography.bodyMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Body weight: ", style = MaterialTheme.typography.labelMedium)
            Text("${spec.bodyWeight}", style = MaterialTheme.typography.bodyMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tight spacing: ", style = MaterialTheme.typography.labelMedium)
            Text(if (spec.tightLetterSpacing) "Yes" else "No", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Display", style = MaterialTheme.typography.displaySmall)
        Text("Headline", style = MaterialTheme.typography.headlineMedium)
        Text("Title", style = MaterialTheme.typography.titleLarge)
        Text("Body text sample", style = MaterialTheme.typography.bodyLarge)
        Text("Label", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ShapePreview(spec: HalogenThemeSpec) {
    val shapes = remember(spec) { ThemeExpander.expandShapes(spec) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Corner style: ", style = MaterialTheme.typography.labelMedium)
            Text(spec.cornerStyle, style = MaterialTheme.typography.bodyMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Corner scale: ", style = MaterialTheme.typography.labelMedium)
            Text("${spec.cornerScale}x", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            listOf(
                "XS" to shapes.extraSmall,
                "S" to shapes.small,
                "M" to shapes.medium,
                "L" to shapes.large,
                "XL" to shapes.extraLarge,
            ).forEach { (label, radiusDp) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(radiusDp.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(label, style = MaterialTheme.typography.labelSmall)
                    Text("${radiusDp}dp", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
