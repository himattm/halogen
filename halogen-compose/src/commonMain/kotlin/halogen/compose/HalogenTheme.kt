package halogen.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import halogen.ExpandedTheme
import halogen.HalogenConfig
import halogen.HalogenExtensions
import halogen.HalogenShapes
import halogen.HalogenThemeSpec
import halogen.ThemeExpander
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Animates all 49 color roles in a [ColorScheme] toward the [target] values.
 *
 * On [initialSnap] (first theme load), all colors snap instantly to avoid a
 * fade-in from the default M3 palette.
 */
@Composable
private fun animateColorScheme(
    target: ColorScheme,
    animationSpec: AnimationSpec<Color>,
    initialSnap: Boolean,
): ColorScheme {
    val spec = if (initialSnap) snap() else animationSpec
    return ColorScheme(
        primary = animateColorAsState(target.primary, spec).value,
        onPrimary = animateColorAsState(target.onPrimary, spec).value,
        primaryContainer = animateColorAsState(target.primaryContainer, spec).value,
        onPrimaryContainer = animateColorAsState(target.onPrimaryContainer, spec).value,
        inversePrimary = animateColorAsState(target.inversePrimary, spec).value,
        primaryFixed = animateColorAsState(target.primaryFixed, spec).value,
        primaryFixedDim = animateColorAsState(target.primaryFixedDim, spec).value,
        onPrimaryFixed = animateColorAsState(target.onPrimaryFixed, spec).value,
        onPrimaryFixedVariant = animateColorAsState(target.onPrimaryFixedVariant, spec).value,
        secondary = animateColorAsState(target.secondary, spec).value,
        onSecondary = animateColorAsState(target.onSecondary, spec).value,
        secondaryContainer = animateColorAsState(target.secondaryContainer, spec).value,
        onSecondaryContainer = animateColorAsState(target.onSecondaryContainer, spec).value,
        secondaryFixed = animateColorAsState(target.secondaryFixed, spec).value,
        secondaryFixedDim = animateColorAsState(target.secondaryFixedDim, spec).value,
        onSecondaryFixed = animateColorAsState(target.onSecondaryFixed, spec).value,
        onSecondaryFixedVariant = animateColorAsState(target.onSecondaryFixedVariant, spec).value,
        tertiary = animateColorAsState(target.tertiary, spec).value,
        onTertiary = animateColorAsState(target.onTertiary, spec).value,
        tertiaryContainer = animateColorAsState(target.tertiaryContainer, spec).value,
        onTertiaryContainer = animateColorAsState(target.onTertiaryContainer, spec).value,
        tertiaryFixed = animateColorAsState(target.tertiaryFixed, spec).value,
        tertiaryFixedDim = animateColorAsState(target.tertiaryFixedDim, spec).value,
        onTertiaryFixed = animateColorAsState(target.onTertiaryFixed, spec).value,
        onTertiaryFixedVariant = animateColorAsState(target.onTertiaryFixedVariant, spec).value,
        error = animateColorAsState(target.error, spec).value,
        onError = animateColorAsState(target.onError, spec).value,
        errorContainer = animateColorAsState(target.errorContainer, spec).value,
        onErrorContainer = animateColorAsState(target.onErrorContainer, spec).value,
        surface = animateColorAsState(target.surface, spec).value,
        onSurface = animateColorAsState(target.onSurface, spec).value,
        surfaceVariant = animateColorAsState(target.surfaceVariant, spec).value,
        onSurfaceVariant = animateColorAsState(target.onSurfaceVariant, spec).value,
        surfaceTint = animateColorAsState(target.surfaceTint, spec).value,
        surfaceBright = animateColorAsState(target.surfaceBright, spec).value,
        surfaceDim = animateColorAsState(target.surfaceDim, spec).value,
        surfaceContainer = animateColorAsState(target.surfaceContainer, spec).value,
        surfaceContainerHigh = animateColorAsState(target.surfaceContainerHigh, spec).value,
        surfaceContainerHighest = animateColorAsState(target.surfaceContainerHighest, spec).value,
        surfaceContainerLow = animateColorAsState(target.surfaceContainerLow, spec).value,
        surfaceContainerLowest = animateColorAsState(target.surfaceContainerLowest, spec).value,
        background = animateColorAsState(target.background, spec).value,
        onBackground = animateColorAsState(target.onBackground, spec).value,
        inverseSurface = animateColorAsState(target.inverseSurface, spec).value,
        inverseOnSurface = animateColorAsState(target.inverseOnSurface, spec).value,
        outline = animateColorAsState(target.outline, spec).value,
        outlineVariant = animateColorAsState(target.outlineVariant, spec).value,
        scrim = animateColorAsState(target.scrim, spec).value,
    )
}

/**
 * Animates 5 corner radii from [HalogenShapes] and returns the animated [Shapes].
 *
 * On [initialSnap] (first theme load), shapes snap instantly.
 */
@Composable
private fun animateShapes(
    targetShapes: HalogenShapes?,
    animationSpec: AnimationSpec<Float>,
    initialSnap: Boolean,
): Shapes {
    if (targetShapes == null) return Shapes()
    val spec = if (initialSnap) snap<Float>() else animationSpec
    val extraSmall by animateFloatAsState(targetShapes.extraSmall, spec)
    val small by animateFloatAsState(targetShapes.small, spec)
    val medium by animateFloatAsState(targetShapes.medium, spec)
    val large by animateFloatAsState(targetShapes.large, spec)
    val extraLarge by animateFloatAsState(targetShapes.extraLarge, spec)
    return Shapes(
        extraSmall = RoundedCornerShape(extraSmall.dp),
        small = RoundedCornerShape(small.dp),
        medium = RoundedCornerShape(medium.dp),
        large = RoundedCornerShape(large.dp),
        extraLarge = RoundedCornerShape(extraLarge.dp),
    )
}

/**
 * Applies a [HalogenThemeSpec] as a Material 3 theme to the given [content].
 *
 * Both light and dark color schemes are expanded upfront from the spec, so toggling
 * system dark mode is instant — no re-expansion or LLM call needed.
 *
 * Color and shape transitions are animated by default with a smooth 400ms tween.
 * Pass a custom [AnimationSpec] to change the transition style, or [snap] to disable.
 *
 * @param spec The compact theme specification produced by the LLM, or `null` for defaults.
 * @param darkTheme Whether to use the dark variant of the color scheme.
 * @param config Controls chroma levels and palette behavior. See [HalogenConfig].
 * @param colorAnimationSpec Animation spec for color transitions. Default: 400ms tween.
 * @param shapeAnimationSpec Animation spec for shape corner radius transitions. Default: 400ms tween.
 * @param content The composable content to theme.
 */
@Composable
public fun HalogenTheme(
    spec: HalogenThemeSpec? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    config: HalogenConfig = HalogenConfig.Default,
    colorAnimationSpec: AnimationSpec<Color> = HalogenThemeDefaults.colorAnimation,
    shapeAnimationSpec: AnimationSpec<Float> = HalogenThemeDefaults.shapeAnimation,
    content: @Composable () -> Unit,
) {
    val expandedTheme by produceState<ExpandedTheme?>(null, spec, config) {
        value = if (spec != null) {
            withContext(Dispatchers.Default) { ThemeExpander.expand(spec, config) }
        } else {
            null
        }
    }

    // Track whether this is the first theme we've seen. If so, snap instead of
    // animating to avoid a fade-in from the M3 default palette.
    var hasSeenTheme by remember { mutableStateOf(false) }
    val initialSnap = !hasSeenTheme
    if (expandedTheme != null) hasSeenTheme = true

    val targetColorScheme = remember(expandedTheme, darkTheme, config.useGeneratedColors) {
        val theme = expandedTheme
        if (theme != null && config.useGeneratedColors) {
            if (darkTheme) theme.darkColorScheme.toMaterial3()
            else theme.lightColorScheme.toMaterial3()
        } else {
            if (darkTheme) darkColorScheme() else lightColorScheme()
        }
    }

    val colorScheme = animateColorScheme(targetColorScheme, colorAnimationSpec, initialSnap)

    val typography = remember(expandedTheme, config.useGeneratedTypography) {
        if (config.useGeneratedTypography) {
            expandedTheme?.typography?.toMaterial3() ?: Typography()
        } else {
            Typography()
        }
    }

    val targetShapes = if (config.useGeneratedShapes) expandedTheme?.shapes else null
    val shapes = animateShapes(targetShapes, shapeAnimationSpec, initialSnap)

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
 * @param colorAnimationSpec Animation spec for color transitions. Default: 400ms tween.
 * @param shapeAnimationSpec Animation spec for shape corner radius transitions. Default: 400ms tween.
 * @param themeWrapper A composable that receives the expanded theme, dark mode flag,
 *   and content. It should apply your custom theme and call [content].
 * @param content The composable content to theme.
 */
@Composable
public fun HalogenTheme(
    spec: HalogenThemeSpec?,
    darkTheme: Boolean = isSystemInDarkTheme(),
    config: HalogenConfig = HalogenConfig.Default,
    colorAnimationSpec: AnimationSpec<Color> = HalogenThemeDefaults.colorAnimation,
    shapeAnimationSpec: AnimationSpec<Float> = HalogenThemeDefaults.shapeAnimation,
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

/**
 * Default values for [HalogenTheme] animation parameters.
 */
public object HalogenThemeDefaults {
    /** Default color transition: smooth 400ms tween. */
    public val colorAnimation: AnimationSpec<Color> = tween(durationMillis = 400)

    /** Default shape transition: smooth 400ms tween. */
    public val shapeAnimation: AnimationSpec<Float> = tween(durationMillis = 400)
}
