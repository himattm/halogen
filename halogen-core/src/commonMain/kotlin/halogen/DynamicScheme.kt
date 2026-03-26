package halogen

import halogen.color.TonalPalette

/**
 * Maps tonal palettes to all Material 3 color roles for light and dark modes.
 *
 * Follows the official M3 dynamic color specification, including the distinction
 * between neutral (surfaces) and neutral-variant (outlines, surface variants).
 */
internal object DynamicScheme {

    /**
     * Build a complete [HalogenColorScheme] from six tonal palettes and a mode flag.
     *
     * @param isDark Whether to use dark-mode tone mappings.
     * @param primaryPalette Tonal palette for primary roles (chroma ~36).
     * @param secondaryPalette Tonal palette for secondary roles (chroma ~16).
     * @param tertiaryPalette Tonal palette for tertiary roles (chroma ~24).
     * @param neutralPalette Tonal palette for surfaces (chroma ~6).
     * @param neutralVariantPalette Tonal palette for outlines and surface variants (chroma ~8).
     * @param errorPalette Tonal palette for error roles (hue 25, chroma 84).
     */
    fun buildColorScheme(
        isDark: Boolean,
        primaryPalette: TonalPalette,
        secondaryPalette: TonalPalette,
        tertiaryPalette: TonalPalette,
        neutralPalette: TonalPalette,
        neutralVariantPalette: TonalPalette,
        errorPalette: TonalPalette,
    ): HalogenColorScheme {
        val tones = if (isDark) DarkTones else LightTones
        return buildFromTones(
            tones, primaryPalette, secondaryPalette, tertiaryPalette,
            neutralPalette, neutralVariantPalette, errorPalette,
        )
    }

    /**
     * Tone mappings for each M3 color role. Light and dark modes differ only in these values.
     */
    private class ToneMap(
        val primary: Int,
        val onPrimary: Int,
        val primaryContainer: Int,
        val onPrimaryContainer: Int,
        val inversePrimary: Int,
        val primaryFixed: Int,
        val primaryFixedDim: Int,
        val onPrimaryFixed: Int,
        val onPrimaryFixedVariant: Int,
        val secondary: Int,
        val onSecondary: Int,
        val secondaryContainer: Int,
        val onSecondaryContainer: Int,
        val secondaryFixed: Int,
        val secondaryFixedDim: Int,
        val onSecondaryFixed: Int,
        val onSecondaryFixedVariant: Int,
        val tertiary: Int,
        val onTertiary: Int,
        val tertiaryContainer: Int,
        val onTertiaryContainer: Int,
        val tertiaryFixed: Int,
        val tertiaryFixedDim: Int,
        val onTertiaryFixed: Int,
        val onTertiaryFixedVariant: Int,
        val error: Int,
        val onError: Int,
        val errorContainer: Int,
        val onErrorContainer: Int,
        val surface: Int,
        val onSurface: Int,
        val surfaceTint: Int,
        val surfaceBright: Int,
        val surfaceDim: Int,
        val surfaceContainer: Int,
        val surfaceContainerHigh: Int,
        val surfaceContainerHighest: Int,
        val surfaceContainerLow: Int,
        val surfaceContainerLowest: Int,
        val background: Int,
        val onBackground: Int,
        val surfaceVariant: Int,
        val onSurfaceVariant: Int,
        val outline: Int,
        val outlineVariant: Int,
        val inverseSurface: Int,
        val inverseOnSurface: Int,
        val scrim: Int,
    )

    private val LightTones = ToneMap(
        primary = 40, onPrimary = 100, primaryContainer = 90, onPrimaryContainer = 30,
        inversePrimary = 80, primaryFixed = 90, primaryFixedDim = 80,
        onPrimaryFixed = 10, onPrimaryFixedVariant = 30,
        secondary = 40, onSecondary = 100, secondaryContainer = 90, onSecondaryContainer = 30,
        secondaryFixed = 90, secondaryFixedDim = 80, onSecondaryFixed = 10, onSecondaryFixedVariant = 30,
        tertiary = 40, onTertiary = 100, tertiaryContainer = 90, onTertiaryContainer = 30,
        tertiaryFixed = 90, tertiaryFixedDim = 80, onTertiaryFixed = 10, onTertiaryFixedVariant = 30,
        error = 40, onError = 100, errorContainer = 90, onErrorContainer = 30,
        surface = 98, onSurface = 10, surfaceTint = 40, surfaceBright = 98, surfaceDim = 87,
        surfaceContainer = 94, surfaceContainerHigh = 92, surfaceContainerHighest = 90,
        surfaceContainerLow = 96, surfaceContainerLowest = 100, background = 98, onBackground = 10,
        surfaceVariant = 90, onSurfaceVariant = 30, outline = 50, outlineVariant = 80,
        inverseSurface = 20, inverseOnSurface = 95, scrim = 0,
    )

    private val DarkTones = ToneMap(
        primary = 80, onPrimary = 20, primaryContainer = 30, onPrimaryContainer = 90,
        inversePrimary = 40, primaryFixed = 90, primaryFixedDim = 80,
        onPrimaryFixed = 10, onPrimaryFixedVariant = 30,
        secondary = 80, onSecondary = 20, secondaryContainer = 30, onSecondaryContainer = 90,
        secondaryFixed = 90, secondaryFixedDim = 80, onSecondaryFixed = 10, onSecondaryFixedVariant = 30,
        tertiary = 80, onTertiary = 20, tertiaryContainer = 30, onTertiaryContainer = 90,
        tertiaryFixed = 90, tertiaryFixedDim = 80, onTertiaryFixed = 10, onTertiaryFixedVariant = 30,
        error = 80, onError = 20, errorContainer = 30, onErrorContainer = 90,
        surface = 6, onSurface = 90, surfaceTint = 80, surfaceBright = 24, surfaceDim = 6,
        surfaceContainer = 12, surfaceContainerHigh = 17, surfaceContainerHighest = 22,
        surfaceContainerLow = 10, surfaceContainerLowest = 4, background = 6, onBackground = 90,
        surfaceVariant = 30, onSurfaceVariant = 80, outline = 60, outlineVariant = 30,
        inverseSurface = 90, inverseOnSurface = 20, scrim = 0,
    )

    private fun buildFromTones(
        t: ToneMap,
        p: TonalPalette,
        s: TonalPalette,
        ter: TonalPalette,
        n: TonalPalette,
        nv: TonalPalette,
        e: TonalPalette,
    ): HalogenColorScheme = HalogenColorScheme(
        primary = p.tone(t.primary),
        onPrimary = p.tone(t.onPrimary),
        primaryContainer = p.tone(t.primaryContainer),
        onPrimaryContainer = p.tone(t.onPrimaryContainer),
        inversePrimary = p.tone(t.inversePrimary),
        primaryFixed = p.tone(t.primaryFixed),
        primaryFixedDim = p.tone(t.primaryFixedDim),
        onPrimaryFixed = p.tone(t.onPrimaryFixed),
        onPrimaryFixedVariant = p.tone(t.onPrimaryFixedVariant),
        secondary = s.tone(t.secondary),
        onSecondary = s.tone(t.onSecondary),
        secondaryContainer = s.tone(t.secondaryContainer),
        onSecondaryContainer = s.tone(t.onSecondaryContainer),
        secondaryFixed = s.tone(t.secondaryFixed),
        secondaryFixedDim = s.tone(t.secondaryFixedDim),
        onSecondaryFixed = s.tone(t.onSecondaryFixed),
        onSecondaryFixedVariant = s.tone(t.onSecondaryFixedVariant),
        tertiary = ter.tone(t.tertiary),
        onTertiary = ter.tone(t.onTertiary),
        tertiaryContainer = ter.tone(t.tertiaryContainer),
        onTertiaryContainer = ter.tone(t.onTertiaryContainer),
        tertiaryFixed = ter.tone(t.tertiaryFixed),
        tertiaryFixedDim = ter.tone(t.tertiaryFixedDim),
        onTertiaryFixed = ter.tone(t.onTertiaryFixed),
        onTertiaryFixedVariant = ter.tone(t.onTertiaryFixedVariant),
        error = e.tone(t.error),
        onError = e.tone(t.onError),
        errorContainer = e.tone(t.errorContainer),
        onErrorContainer = e.tone(t.onErrorContainer),
        surface = n.tone(t.surface),
        onSurface = n.tone(t.onSurface),
        surfaceTint = p.tone(t.surfaceTint),
        surfaceBright = n.tone(t.surfaceBright),
        surfaceDim = n.tone(t.surfaceDim),
        surfaceContainer = n.tone(t.surfaceContainer),
        surfaceContainerHigh = n.tone(t.surfaceContainerHigh),
        surfaceContainerHighest = n.tone(t.surfaceContainerHighest),
        surfaceContainerLow = n.tone(t.surfaceContainerLow),
        surfaceContainerLowest = n.tone(t.surfaceContainerLowest),
        background = n.tone(t.background),
        onBackground = n.tone(t.onBackground),
        surfaceVariant = nv.tone(t.surfaceVariant),
        onSurfaceVariant = nv.tone(t.onSurfaceVariant),
        outline = nv.tone(t.outline),
        outlineVariant = nv.tone(t.outlineVariant),
        inverseSurface = n.tone(t.inverseSurface),
        inverseOnSurface = n.tone(t.inverseOnSurface),
        scrim = n.tone(t.scrim),
    )
}
