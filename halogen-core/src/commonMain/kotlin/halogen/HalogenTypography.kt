package halogen

/**
 * Typography configuration derived from [HalogenThemeSpec].
 * Platform adapters map this to concrete font families.
 */
public data class HalogenTypography(
    val fontMood: String,
    val headingWeight: Int,
    val bodyWeight: Int,
    val tightLetterSpacing: Boolean,
) {
    /**
     * Maps the [fontMood] to a generic font family hint.
     * Platform code should map these hints to actual font families.
     */
    public fun fontFamilyHint(): String = when (fontMood) {
        "modern" -> "sans-serif"
        "classic" -> "serif"
        "playful" -> "rounded"
        "minimal" -> "thin"
        "mono" -> "monospace"
        else -> "sans-serif"
    }
}
