package halogen

import kotlin.math.pow

/**
 * Describes a contrast failure between two color roles.
 */
internal data class ContrastIssue(
    val rolePair: String,
    val foreground: Int,
    val background: Int,
    val ratio: Double,
    val requiredRatio: Double,
)

/**
 * Result of validating a color scheme for WCAG contrast compliance.
 */
internal sealed class ValidationResult {
    /** All checked pairs meet the required contrast ratio. */
    data object Pass : ValidationResult()

    /** One or more pairs fail to meet the required contrast ratio. */
    data class Fail(val issues: List<ContrastIssue>) : ValidationResult()
}

/**
 * Validates that a [HalogenColorScheme] meets WCAG 2.1 AA contrast requirements
 * for the standard foreground/background role pairs.
 */
internal object ContrastValidator {

    /** WCAG AA minimum contrast ratio for normal text. */
    private const val AA_RATIO: Double = 4.5

    /** Role pairs to validate: each pair is (foreground role, background role). */
    private data class RolePair(
        val name: String,
        val foreground: (HalogenColorScheme) -> Int,
        val background: (HalogenColorScheme) -> Int,
        val requiredRatio: Double = AA_RATIO,
    )

    private val rolePairs: List<RolePair> = listOf(
        RolePair("primary/onPrimary", { it.onPrimary }, { it.primary }),
        RolePair("secondary/onSecondary", { it.onSecondary }, { it.secondary }),
        RolePair("tertiary/onTertiary", { it.onTertiary }, { it.tertiary }),
        RolePair("error/onError", { it.onError }, { it.error }),
        RolePair("primaryContainer/onPrimaryContainer", { it.onPrimaryContainer }, { it.primaryContainer }),
        RolePair("secondaryContainer/onSecondaryContainer", { it.onSecondaryContainer }, { it.secondaryContainer }),
        RolePair("tertiaryContainer/onTertiaryContainer", { it.onTertiaryContainer }, { it.tertiaryContainer }),
        RolePair("errorContainer/onErrorContainer", { it.onErrorContainer }, { it.errorContainer }),
        RolePair("surface/onSurface", { it.onSurface }, { it.surface }),
        RolePair("surfaceVariant/onSurfaceVariant", { it.onSurfaceVariant }, { it.surfaceVariant }),
        RolePair("background/onBackground", { it.onBackground }, { it.background }),
        RolePair("inverseSurface/inverseOnSurface", { it.inverseOnSurface }, { it.inverseSurface }),
    )

    /**
     * Validate all standard foreground/background pairs in the scheme.
     *
     * @return [ValidationResult.Pass] if all pairs meet AA contrast, or
     *         [ValidationResult.Fail] with the list of failing pairs.
     */
    fun validate(scheme: HalogenColorScheme): ValidationResult {
        val issues = mutableListOf<ContrastIssue>()

        for (pair in rolePairs) {
            val fg = pair.foreground(scheme)
            val bg = pair.background(scheme)
            val ratio = contrastRatio(fg, bg)
            if (ratio < pair.requiredRatio) {
                issues.add(
                    ContrastIssue(
                        rolePair = pair.name,
                        foreground = fg,
                        background = bg,
                        ratio = ratio,
                        requiredRatio = pair.requiredRatio,
                    ),
                )
            }
        }

        return if (issues.isEmpty()) ValidationResult.Pass else ValidationResult.Fail(issues)
    }

    /**
     * Compute the relative luminance of an ARGB color.
     *
     * Uses the sRGB linearization formula per WCAG 2.1:
     * - Normalize each channel to 0.0-1.0.
     * - Linearize: if value <= 0.04045, divide by 12.92;
     *   otherwise, ((value + 0.055) / 1.055) ^ 2.4.
     * - L = 0.2126 * R + 0.7152 * G + 0.0722 * B.
     */
    fun relativeLuminance(argb: Int): Double {
        val r = linearizeInt((argb shr 16) and 0xFF)
        val g = linearizeInt((argb shr 8) and 0xFF)
        val b = linearizeInt(argb and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /**
     * Compute the WCAG contrast ratio between two ARGB colors.
     * Result is >= 1.0, with 21.0 being the maximum (black on white).
     */
    fun contrastRatio(argb1: Int, argb2: Int): Double {
        val lum1 = relativeLuminance(argb1)
        val lum2 = relativeLuminance(argb2)
        val lighter = maxOf(lum1, lum2)
        val darker = minOf(lum1, lum2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    /**
     * Check if the foreground/background pair meets WCAG AA (4.5:1) for normal text.
     */
    fun meetsAA(foreground: Int, background: Int): Boolean {
        return contrastRatio(foreground, background) >= AA_RATIO
    }

    /**
     * Pre-computed Lookup Table (LUT) for sRGB linearization.
     *
     * Optimization: The relative luminance calculation requires converting 8-bit color components (0-255)
     * from sRGB to linear space. Previously, this was done dynamically using floating-point math and expensive
     * `Double.pow(2.4)` operations. Since the input domain is strictly limited to 256 discrete values,
     * we can pre-compute all possible results into this array. This reduces the calculation from complex
     * math to a simple O(1) array lookup, significantly improving performance in this KMP hot path.
     */
    private val LINEARIZE_LUT = DoubleArray(256) { i ->
        val component = i / 255.0
        if (component <= 0.04045) {
            component / 12.92
        } else {
            ((component + 0.055) / 1.055).pow(2.4)
        }
    }

    private fun linearizeInt(component: Int): Double {
        return LINEARIZE_LUT[component and 0xFF]
    }
}
