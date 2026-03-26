package halogen

import halogen.color.Hct
import halogen.color.TonalPalette

/**
 * The tonal palettes derived from a [HalogenThemeSpec], following M3's palette structure.
 */
public data class HalogenPalette(
    val primary: TonalPalette,
    val secondary: TonalPalette,
    val tertiary: TonalPalette,
    val neutral: TonalPalette,
    val neutralVariant: TonalPalette,
    val error: TonalPalette,
)

/**
 * A fully expanded theme: both color schemes, typography, and shapes.
 */
public data class ExpandedTheme(
    val palette: HalogenPalette,
    val lightColorScheme: HalogenColorScheme,
    val darkColorScheme: HalogenColorScheme,
    val typography: HalogenTypography,
    val shapes: HalogenShapes,
)

/**
 * Orchestrates the expansion of a compact [HalogenThemeSpec] into a full
 * Material 3 theme with light and dark schemes, typography, and shapes.
 *
 * Follows M3's SchemeTonalSpot approach:
 * - The LLM provides hue direction via seed colors
 * - Chroma is capped to M3-appropriate levels per palette role
 * - A neutral-variant palette is derived for outlines and surface variants
 * - Light/dark modes use the same palettes with different tone mappings
 *
 * Chroma values (following SchemeTonalSpot):
 * - Primary: max 48 (vibrant but not neon — slightly higher than M3's 36 for more personality)
 * - Secondary: max 16 (muted, supportive)
 * - Tertiary: max 24 (accent, moderate)
 * - Neutral: 6 (near-gray for surfaces)
 * - Neutral Variant: 8 (slightly tinted for outlines/surface variants)
 * - Error: fixed hue 25, chroma 84
 */
public object ThemeExpander {

    /**
     * Fully expand a [HalogenThemeSpec] into an [ExpandedTheme].
     *
     * @param config Controls chroma levels for each palette role. Use [HalogenConfig.Default]
     *   for M3 SchemeTonalSpot behavior, or customize for your brand.
     */
    public fun expand(
        spec: HalogenThemeSpec,
        config: HalogenConfig = HalogenConfig.Default,
    ): ExpandedTheme {
        val shared = buildSharedPalettes(spec, config)
        val lightNeutrals = buildNeutralPalettes(spec, isDark = false, config = config)
        val darkNeutrals = buildNeutralPalettes(spec, isDark = true, config = config)
        val lightPalette = shared.withNeutrals(lightNeutrals)
        val darkPalette = shared.withNeutrals(darkNeutrals)
        return ExpandedTheme(
            palette = lightPalette,
            lightColorScheme = buildScheme(lightPalette, isDark = false),
            darkColorScheme = buildScheme(darkPalette, isDark = true),
            typography = expandTypography(spec),
            shapes = expandShapes(spec),
        )
    }

    /**
     * Expand only the color scheme for a given mode.
     *
     * @param config Controls chroma levels for each palette role.
     */
    public fun expandColors(
        spec: HalogenThemeSpec,
        isDark: Boolean,
        config: HalogenConfig = HalogenConfig.Default,
    ): HalogenColorScheme {
        val shared = buildSharedPalettes(spec, config)
        val neutrals = buildNeutralPalettes(spec, isDark, config)
        val palette = shared.withNeutrals(neutrals)
        return buildScheme(palette, isDark)
    }

    /**
     * Expand the typography configuration from a spec.
     */
    public fun expandTypography(spec: HalogenThemeSpec): HalogenTypography {
        return HalogenTypography(
            fontMood = spec.fontMood,
            headingWeight = spec.headingWeight.coerceIn(100, 900),
            bodyWeight = spec.bodyWeight.coerceIn(100, 900),
            tightLetterSpacing = spec.tightLetterSpacing,
        )
    }

    /**
     * Expand the shapes configuration from a spec.
     */
    public fun expandShapes(spec: HalogenThemeSpec): HalogenShapes {
        return HalogenShapes.fromSpec(spec.cornerStyle, spec.cornerScale)
    }

    /**
     * Parse a hex color string like "#1A73E8" to an ARGB integer (0xFF1A73E8).
     */
    internal fun parseHexToArgb(hex: String): Int {
        require(hex.startsWith("#") && hex.length == 7) {
            "Invalid hex color: \"$hex\". Expected format: #RRGGBB"
        }
        val rgb = hex.substring(1).toLong(16).toInt()
        return rgb or (0xFF shl 24).toInt()
    }

    /**
     * Convert an ARGB integer to a hex color string like "#1A73E8".
     */
    public fun argbToHex(argb: Int): String {
        val rgb = argb and 0xFFFFFF
        return "#" + rgb.toString(16).padStart(6, '0').uppercase()
    }

    private fun buildScheme(palette: HalogenPalette, isDark: Boolean): HalogenColorScheme {
        return DynamicScheme.buildColorScheme(
            isDark = isDark,
            primaryPalette = palette.primary,
            secondaryPalette = palette.secondary,
            tertiaryPalette = palette.tertiary,
            neutralPalette = palette.neutral,
            neutralVariantPalette = palette.neutralVariant,
            errorPalette = palette.error,
        )
    }

    /**
     * Minimum chroma threshold below which a seed color is considered "achromatic"
     * (effectively gray). When hue is unreliable, we look at other seeds for hue hints.
     */
    private const val ACHROMATIC_THRESHOLD = 5.0

    /** Holds the four palettes that are identical between light and dark modes. */
    private data class SharedPalettes(
        val primary: TonalPalette,
        val secondary: TonalPalette,
        val tertiary: TonalPalette,
        val error: TonalPalette,
    ) {
        fun withNeutrals(neutrals: NeutralPalettes): HalogenPalette = HalogenPalette(
            primary = primary,
            secondary = secondary,
            tertiary = tertiary,
            neutral = neutrals.neutral,
            neutralVariant = neutrals.neutralVariant,
            error = error,
        )
    }

    /** Holds the mode-specific neutral palettes. */
    private data class NeutralPalettes(
        val neutral: TonalPalette,
        val neutralVariant: TonalPalette,
    )

    private fun buildSharedPalettes(spec: HalogenThemeSpec, config: HalogenConfig): SharedPalettes {
        val primaryHct = Hct.fromInt(parseHexToArgb(spec.primary))
        val secondaryHct = Hct.fromInt(parseHexToArgb(spec.secondary))
        val tertiaryHct = Hct.fromInt(parseHexToArgb(spec.tertiary))
        val errorHct = Hct.fromInt(parseHexToArgb(spec.error))

        // If the primary seed is near-achromatic (gray/white), its hue is unreliable.
        // Fall back to the most chromatic non-neutral seed for hue direction,
        // then enforce a minimum chroma so the theme has visible color.
        val primaryHue: Double
        val primaryChroma: Double
        if (primaryHct.chroma < ACHROMATIC_THRESHOLD) {
            val hueHints = listOf(tertiaryHct, secondaryHct, errorHct)
                .filter { it.chroma >= ACHROMATIC_THRESHOLD }
                .maxByOrNull { it.chroma }
            primaryHue = hueHints?.hue ?: primaryHct.hue
            primaryChroma = config.primaryMinChroma
        } else {
            primaryHue = primaryHct.hue
            primaryChroma = primaryHct.chroma
                .coerceAtLeast(config.primaryMinChroma)
                .coerceAtMost(config.primaryMaxChroma)
        }

        // Same fallback for secondary — if achromatic, derive from primary hue
        val secondaryHue = if (secondaryHct.chroma < ACHROMATIC_THRESHOLD) {
            primaryHue
        } else {
            secondaryHct.hue
        }

        // Error palette: fixed M3 red by default, or LLM's hue if configured
        val errorHue = if (config.useFixedError) {
            config.errorHue ?: errorHct.hue
        } else {
            errorHct.hue
        }
        val errorChroma = if (config.useFixedError) {
            config.errorChroma
        } else {
            errorHct.chroma.coerceAtMost(config.errorChroma)
        }

        return SharedPalettes(
            primary = TonalPalette.fromHueAndChroma(primaryHue, primaryChroma),
            secondary = TonalPalette.fromHueAndChroma(
                secondaryHue,
                secondaryHct.chroma.coerceAtMost(config.secondaryMaxChroma)
                    .coerceAtLeast(config.secondaryMaxChroma * 0.5),
            ),
            tertiary = TonalPalette.fromHueAndChroma(
                tertiaryHct.hue,
                tertiaryHct.chroma.coerceAtMost(config.tertiaryMaxChroma)
                    .coerceAtLeast(config.tertiaryMaxChroma * 0.3),
            ),
            error = TonalPalette.fromHueAndChroma(errorHue, errorChroma),
        )
    }

    private fun buildNeutralPalettes(spec: HalogenThemeSpec, isDark: Boolean, config: HalogenConfig): NeutralPalettes {
        val neutralHex = if (isDark) spec.neutralDark else spec.neutralLight
        val neutralHct = Hct.fromInt(parseHexToArgb(neutralHex))
        return NeutralPalettes(
            neutral = TonalPalette.fromHueAndChroma(neutralHct.hue, config.neutralChroma),
            neutralVariant = TonalPalette.fromHueAndChroma(neutralHct.hue, config.neutralVariantChroma),
        )
    }
}
