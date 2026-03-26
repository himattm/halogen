package halogen.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import halogen.ExpandedTheme
import halogen.HalogenConfig
import halogen.HalogenExtensions
import halogen.HalogenThemeSpec
import halogen.ThemeExpander
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Applies a [HalogenThemeSpec] as a Material 3 theme to the given [content].
 *
 * Both light and dark color schemes are expanded upfront from the spec, so toggling
 * system dark mode is instant — no re-expansion or LLM call needed.
 *
 * @param spec The compact theme specification produced by the LLM, or `null` for defaults.
 * @param darkTheme Whether to use the dark variant of the color scheme.
 * @param config Controls chroma levels and palette behavior. See [HalogenConfig].
 * @param content The composable content to theme.
 */
@Composable
public fun HalogenTheme(
    spec: HalogenThemeSpec? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    config: HalogenConfig = HalogenConfig.Default,
    content: @Composable () -> Unit,
) {
    // Expand both light and dark schemes off the main thread when spec changes.
    // Switching dark mode just picks the other pre-computed scheme — zero work.
    val expandedTheme by produceState<ExpandedTheme?>(null, spec, config) {
        value = if (spec != null) {
            withContext(Dispatchers.Default) { ThemeExpander.expand(spec, config) }
        } else {
            null
        }
    }

    val colorScheme = remember(expandedTheme, darkTheme) {
        val theme = expandedTheme
        if (theme != null) {
            if (darkTheme) theme.darkColorScheme.toMaterial3()
            else theme.lightColorScheme.toMaterial3()
        } else {
            if (darkTheme) darkColorScheme() else lightColorScheme()
        }
    }

    val typography = remember(expandedTheme) {
        expandedTheme?.typography?.toMaterial3() ?: Typography()
    }

    val shapes = remember(expandedTheme) {
        expandedTheme?.shapes?.toMaterial3() ?: Shapes()
    }

    val extensions = remember(spec) {
        spec?.extensions?.let { HalogenExtensions(it) }
            ?: HalogenExtensions.empty()
    }

    CompositionLocalProvider(
        LocalHalogenExtensions provides extensions,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content,
        )
    }
}

/**
 * Applies a [HalogenThemeSpec] using a custom theme wrapper instead of Material 3.
 *
 * Use this overload to integrate Halogen with non-Material design systems.
 * The [themeWrapper] receives the [ExpandedTheme] and is responsible for
 * providing the appropriate theme to [content].
 *
 * @param spec The compact theme specification produced by the LLM, or `null` for no theme.
 * @param darkTheme Whether to use the dark variant.
 * @param config Controls chroma levels and palette behavior. See [HalogenConfig].
 * @param themeWrapper A composable that receives the expanded theme, dark mode flag,
 *   and content. It should apply your custom theme and call [content].
 * @param content The composable content to theme.
 */
@Composable
public fun HalogenTheme(
    spec: HalogenThemeSpec?,
    darkTheme: Boolean = isSystemInDarkTheme(),
    config: HalogenConfig = HalogenConfig.Default,
    themeWrapper: @Composable (theme: ExpandedTheme, isDark: Boolean, content: @Composable () -> Unit) -> Unit,
    content: @Composable () -> Unit,
) {
    val expandedTheme by produceState<ExpandedTheme?>(null, spec, config) {
        value = if (spec != null) {
            withContext(Dispatchers.Default) { ThemeExpander.expand(spec, config) }
        } else {
            null
        }
    }

    val extensions = remember(spec) {
        spec?.extensions?.let { HalogenExtensions(it) }
            ?: HalogenExtensions.empty()
    }

    CompositionLocalProvider(
        LocalHalogenExtensions provides extensions,
    ) {
        val theme = expandedTheme
        if (theme != null) {
            themeWrapper(theme, darkTheme, content)
        } else {
            content()
        }
    }
}

/**
 * Convenience object for accessing Halogen-specific theme tokens
 * from within a [HalogenTheme] scope.
 */
public object HalogenTheme {
    /**
     * The current [HalogenExtensions] provided by the nearest [HalogenTheme].
     */
    public val extensions: HalogenExtensions
        @Composable
        get() = LocalHalogenExtensions.current
}
