package halogen.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import halogen.HalogenThemeSpec
import halogen.ThemeExpander

/**
 * Displays a horizontal row of six color swatch circles representing
 * the key seed colors from a [HalogenThemeSpec]: primary, secondary,
 * tertiary, neutral (light or dark based on [isDark]), and error.
 *
 * @param spec The theme spec whose colors to preview.
 * @param modifier Modifier applied to the outer [Row].
 * @param isDark Whether to show the dark-mode neutral color.
 */
@Composable
public fun HalogenColorPreview(
    spec: HalogenThemeSpec,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme(),
) {
    val colors by produceState(emptyList<Int>(), spec, isDark) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val scheme = ThemeExpander.expandColors(spec, isDark = isDark)
            listOf(
                scheme.primary,
                scheme.secondary,
                scheme.tertiary,
                scheme.surface,
                scheme.surfaceVariant,
                scheme.error,
            )
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        colors.forEach { argb ->
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(argb)),
            )
        }
    }
}
