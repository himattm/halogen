package halogen.image

import halogen.HalogenThemeSpec
import halogen.ThemeExpander
import halogen.color.Hct

/**
 * Maps a list of dominant [QuantizedColor]s extracted from an image to a
 * [HalogenThemeSpec] suitable for theme generation.
 *
 * The mapping heuristic selects colors for each seed role based on HCT
 * properties (chroma for vibrancy, tone for lightness, hue distance for variety).
 */
internal object ImageThemeExtractor {

    /** Fixed M3 error color. */
    private const val ERROR_HEX = "#BA1A1A"

    /** Default blue hue used when no colors are available. */
    private const val DEFAULT_HUE = 220.0

    /** Minimum hue separation between secondary and primary. */
    private const val SECONDARY_HUE_GAP = 15.0

    /** Minimum hue separation between tertiary and primary. */
    private const val TERTIARY_HUE_GAP = 30.0

    // v1: Fixed typography and shape values. Future versions may derive these
    // from palette mood (e.g., dark/vibrant -> bolder weight, angular corners).
    private const val DEFAULT_FONT_MOOD = "modern"
    private const val DEFAULT_HEADING_WEIGHT = 600
    private const val DEFAULT_BODY_WEIGHT = 400
    private const val DEFAULT_TIGHT_LETTER_SPACING = false
    private const val DEFAULT_CORNER_STYLE = "rounded"
    private const val DEFAULT_CORNER_SCALE = 1.0f

    /**
     * Map a list of extracted dominant colors to a [HalogenThemeSpec].
     *
     * Handles edge cases: empty list, single color, and two colors by synthesizing
     * missing seed colors from hue shifts of available ones.
     */
    fun mapToSpec(colors: List<QuantizedColor>): HalogenThemeSpec {
        return when {
            colors.isEmpty() -> synthesizeAll(DEFAULT_HUE)
            colors.size == 1 -> mapSingleColor(colors[0])
            colors.size == 2 -> mapTwoColors(colors[0], colors[1])
            else -> mapMultipleColors(colors)
        }
    }

    /**
     * Full mapping when 3+ colors are available.
     */
    private fun mapMultipleColors(colors: List<QuantizedColor>): HalogenThemeSpec {
        val byChroma = colors.sortedByDescending { it.chroma }

        // Primary: highest chroma with tone in 30-70; fallback to highest chroma overall
        val primary = byChroma.firstOrNull { it.tone in 30.0..70.0 } ?: byChroma[0]

        // Secondary: next highest chroma with sufficient hue distance from primary
        val secondary = byChroma.firstOrNull { c ->
            c !== primary && hueDist(c.hue, primary.hue) >= SECONDARY_HUE_GAP
        }

        // Tertiary: next with larger hue distance from primary
        val tertiary = byChroma.firstOrNull { c ->
            c !== primary && c !== secondary && hueDist(c.hue, primary.hue) >= TERTIARY_HUE_GAP
        }

        // Neutral light: lowest-chroma with tone > 85
        val neutralLight = colors
            .filter { it.tone > 85.0 }
            .minByOrNull { it.chroma }

        // Neutral dark: lowest-chroma with tone < 20
        val neutralDark = colors
            .filter { it.tone < 20.0 }
            .minByOrNull { it.chroma }

        return buildSpec(
            primary = primary,
            secondary = secondary,
            tertiary = tertiary,
            neutralLight = neutralLight,
            neutralDark = neutralDark,
            primaryHue = primary.hue,
        )
    }

    /**
     * Single color: use as primary, derive secondary and tertiary by shifting hue.
     */
    private fun mapSingleColor(color: QuantizedColor): HalogenThemeSpec {
        return buildSpec(
            primary = color,
            secondary = null,
            tertiary = null,
            neutralLight = null,
            neutralDark = null,
            primaryHue = color.hue,
        )
    }

    /**
     * Two colors: assign by chroma, derive third from hue shift.
     */
    private fun mapTwoColors(a: QuantizedColor, b: QuantizedColor): HalogenThemeSpec {
        val (primary, secondary) = if (a.chroma >= b.chroma) a to b else b to a
        return buildSpec(
            primary = primary,
            secondary = secondary,
            tertiary = null,
            neutralLight = null,
            neutralDark = null,
            primaryHue = primary.hue,
        )
    }

    /**
     * Synthesize an entire spec from a single hue (used for empty color lists).
     */
    private fun synthesizeAll(hue: Double): HalogenThemeSpec {
        return HalogenThemeSpec(
            primary = hctToHex(hue, 48.0, 50.0),
            secondary = hctToHex((hue + 30.0) % 360, 36.0, 50.0),
            tertiary = hctToHex((hue + 60.0) % 360, 24.0, 50.0),
            neutralLight = hctToHex(hue, 4.0, 96.0),
            neutralDark = hctToHex(hue, 4.0, 8.0),
            error = ERROR_HEX,
            fontMood = DEFAULT_FONT_MOOD,
            headingWeight = DEFAULT_HEADING_WEIGHT,
            bodyWeight = DEFAULT_BODY_WEIGHT,
            tightLetterSpacing = DEFAULT_TIGHT_LETTER_SPACING,
            cornerStyle = DEFAULT_CORNER_STYLE,
            cornerScale = DEFAULT_CORNER_SCALE,
        )
    }

    /**
     * Build a [HalogenThemeSpec] from resolved and possibly null role candidates.
     * Synthesizes any missing roles from the primary hue.
     */
    private fun buildSpec(
        primary: QuantizedColor,
        secondary: QuantizedColor?,
        tertiary: QuantizedColor?,
        neutralLight: QuantizedColor?,
        neutralDark: QuantizedColor?,
        primaryHue: Double,
    ): HalogenThemeSpec {
        val secHex = if (secondary != null) {
            ThemeExpander.argbToHex(secondary.argb)
        } else {
            hctToHex((primaryHue + 30.0) % 360, 36.0, 50.0)
        }

        val terHex = if (tertiary != null) {
            ThemeExpander.argbToHex(tertiary.argb)
        } else {
            hctToHex((primaryHue + 60.0) % 360, 24.0, 50.0)
        }

        val neuLHex = if (neutralLight != null) {
            ThemeExpander.argbToHex(neutralLight.argb)
        } else {
            hctToHex(primaryHue, 4.0, 96.0)
        }

        val neuDHex = if (neutralDark != null) {
            ThemeExpander.argbToHex(neutralDark.argb)
        } else {
            hctToHex(primaryHue, 4.0, 8.0)
        }

        return HalogenThemeSpec(
            primary = ThemeExpander.argbToHex(primary.argb),
            secondary = secHex,
            tertiary = terHex,
            neutralLight = neuLHex,
            neutralDark = neuDHex,
            error = ERROR_HEX,
            fontMood = DEFAULT_FONT_MOOD,
            headingWeight = DEFAULT_HEADING_WEIGHT,
            bodyWeight = DEFAULT_BODY_WEIGHT,
            tightLetterSpacing = DEFAULT_TIGHT_LETTER_SPACING,
            cornerStyle = DEFAULT_CORNER_STYLE,
            cornerScale = DEFAULT_CORNER_SCALE,
        )
    }

    /**
     * Create an ARGB color from HCT components and convert to hex string.
     */
    private fun hctToHex(hue: Double, chroma: Double, tone: Double): String {
        return ThemeExpander.argbToHex(Hct.from(hue, chroma, tone).toInt())
    }

}
