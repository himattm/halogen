package halogen.image

import halogen.HalogenThemeSpec
import halogen.ThemeExpander

/**
 * The result of image color extraction: a list of [QuantizedColor]s representing
 * the dominant colors found in an image, sorted by population descending.
 *
 * Provides convenience methods to convert the palette into a [HalogenThemeSpec]
 * or into an enriched LLM prompt hint.
 */
public data class DominantColors(public val colors: List<QuantizedColor>) {

    /**
     * Map the extracted dominant colors to a [HalogenThemeSpec] by assigning
     * colors to theme seed roles based on HCT properties (chroma, tone, hue distance).
     *
     * Handles edge cases gracefully: empty palettes produce a default blue theme,
     * palettes with fewer than 3 colors synthesize missing roles via hue shifts.
     */
    public fun toSpec(): HalogenThemeSpec {
        return ImageThemeExtractor.mapToSpec(colors)
    }

    /**
     * Format the extracted palette as an enriched LLM prompt hint.
     *
     * The hint includes a one-shot example showing the expected JSON output format,
     * followed by the actual extracted palette with mood descriptors derived from
     * average tone and chroma.
     *
     * Example output:
     * ```
     * I have extracted the dominant colors from an image. Generate a theme inspired by these colors.
     *
     * For example, given palette #1B4332 (40%), #52B788 (35%), #D8F3DC (25%) with a fresh natural mood:
     * {"pri":"#2D6A4F","sec":"#40916C","ter":"#74C69D","neuL":"#F0F7F4","neuD":"#0B1F15","err":"#BA1A1A","font":"modern","hw":500,"bw":400,"ls":false,"cs":"soft","cx":1.2}
     *
     * Now generate for: Image palette: #2A1B3D (45%), #E94560 (30%), #533483 (25%). Dark, vibrant mood.
     * ```
     */
    public fun toHint(): String {
        if (colors.isEmpty()) {
            return "I have extracted the dominant colors from an image but found no significant colors. " +
                "Generate a theme with a calm, neutral palette."
        }

        val paletteStr = colors.joinToString(", ") { color ->
            val hex = ThemeExpander.argbToHex(color.argb)
            val pct = (color.population * 100).toInt()
            "$hex ($pct%)"
        }

        val mood = describeMood()

        return buildString {
            append("I have extracted the dominant colors from an image. ")
            appendLine("Generate a theme inspired by these colors.")
            appendLine()
            append("For example, given palette #1B4332 (40%), #52B788 (35%), #D8F3DC (25%) ")
            appendLine("with a fresh natural mood:")
            appendLine("""{"pri":"#2D6A4F","sec":"#40916C","ter":"#74C69D","neuL":"#F0F7F4","neuD":"#0B1F15","err":"#BA1A1A","font":"modern","hw":500,"bw":400,"ls":false,"cs":"soft","cx":1.2}""")
            appendLine()
            append("Now generate for: Image palette: $paletteStr. $mood mood.")
        }
    }

    /**
     * Describe the mood of the palette based on average tone and chroma.
     *
     * - Tone < 40 = "dark", > 60 = "light", else "mid-tone"
     * - Chroma < 20 = "subdued", < 40 = "moderate", else "vibrant"
     */
    public fun describeMood(): String {
        if (colors.isEmpty()) return "Neutral"

        val totalPop = colors.sumOf { it.population }
        if (totalPop == 0.0) return "Neutral"

        val avgTone = colors.sumOf { it.tone * it.population } / totalPop
        val avgChroma = colors.sumOf { it.chroma * it.population } / totalPop

        val toneDesc = when {
            avgTone < 40.0 -> "dark"
            avgTone > 60.0 -> "light"
            else -> "mid-tone"
        }

        val chromaDesc = when {
            avgChroma < 20.0 -> "subdued"
            avgChroma < 40.0 -> "moderate"
            else -> "vibrant"
        }

        return "$toneDesc, $chromaDesc"
    }

    override fun toString(): String {
        val label = if (colors.size == 1) "color" else "colors"
        return "DominantColors(${colors.size} $label)"
    }
}
