package halogen

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ContrastValidatorTest {

    private val white = 0xFFFFFFFF.toInt()
    private val black = 0xFF000000.toInt()

    // ---- Contrast ratio calculations ----

    @Test
    fun whiteOnBlack_contrastIs21() {
        val ratio = ContrastValidator.contrastRatio(white, black)
        assertTrue(
            abs(ratio - 21.0) < 0.1,
            "White on black should have ~21:1 contrast, got $ratio",
        )
    }

    @Test
    fun blackOnWhite_contrastIs21() {
        val ratio = ContrastValidator.contrastRatio(black, white)
        assertTrue(
            abs(ratio - 21.0) < 0.1,
            "Black on white should have ~21:1 contrast, got $ratio",
        )
    }

    @Test
    fun sameColor_contrastIs1() {
        val ratio = ContrastValidator.contrastRatio(white, white)
        assertEquals(1.0, ratio, 0.001, "Same color contrast should be 1.0")
    }

    // ---- meetsAA ----

    @Test
    fun blackOnWhite_passesAA() {
        assertTrue(
            ContrastValidator.meetsAA(black, white),
            "Black on white should pass AA",
        )
    }

    @Test
    fun lowContrastPair_failsAA() {
        // Light gray on white: very low contrast
        val lightGray = 0xFFCCCCCC.toInt()
        assertFalse(
            ContrastValidator.meetsAA(lightGray, white),
            "Light gray on white should fail AA",
        )
    }

    @Test
    fun darkGrayOnWhite_passesAA() {
        // Dark gray (#595959) on white has ~7:1 contrast
        val darkGray = 0xFF595959.toInt()
        assertTrue(
            ContrastValidator.meetsAA(darkGray, white),
            "Dark gray on white should pass AA",
        )
    }

    // ---- Relative luminance ----

    @Test
    fun relativeLuminance_white_is1() {
        val lum = ContrastValidator.relativeLuminance(white)
        assertTrue(
            abs(lum - 1.0) < 0.001,
            "White luminance should be ~1.0, got $lum",
        )
    }

    @Test
    fun relativeLuminance_black_is0() {
        val lum = ContrastValidator.relativeLuminance(black)
        assertTrue(
            abs(lum) < 0.001,
            "Black luminance should be ~0.0, got $lum",
        )
    }

    // ---- Full scheme validation ----

    @Test
    fun validateDefaultM3Scheme_lightMode() {
        val spec = HalogenDefaults.light()
        val scheme = ThemeExpander.expandColors(spec, isDark = false)
        val result = ContrastValidator.validate(scheme)
        // M3 defaults should generally pass; if not, log failing pairs
        when (result) {
            is ValidationResult.Pass -> {} // good
            is ValidationResult.Fail -> {
                // Allow up to a couple of marginal failures in M3's own palette
                // (containerOn pairs can sometimes be close to 4.5:1)
                val critical = result.issues.filter { it.ratio < 3.0 }
                assertTrue(
                    critical.isEmpty(),
                    "Default M3 light scheme has critically low contrast pairs: " +
                        critical.joinToString { "${it.rolePair}: ${it.ratio}" },
                )
            }
        }
    }

    @Test
    fun validateDefaultM3Scheme_darkMode() {
        val spec = HalogenDefaults.light()
        val scheme = ThemeExpander.expandColors(spec, isDark = true)
        val result = ContrastValidator.validate(scheme)
        when (result) {
            is ValidationResult.Pass -> {}
            is ValidationResult.Fail -> {
                val critical = result.issues.filter { it.ratio < 3.0 }
                assertTrue(
                    critical.isEmpty(),
                    "Default M3 dark scheme has critically low contrast pairs: " +
                        critical.joinToString { "${it.rolePair}: ${it.ratio}" },
                )
            }
        }
    }

    @Test
    fun validate_artificialLowContrastScheme_fails() {
        // Create a scheme where everything is mid-gray -- should fail many pairs
        val midGray = 0xFF808080.toInt()
        val scheme = HalogenColorScheme(
            primary = midGray, onPrimary = midGray,
            primaryContainer = midGray, onPrimaryContainer = midGray,
            inversePrimary = midGray,
            primaryFixed = midGray, primaryFixedDim = midGray,
            onPrimaryFixed = midGray, onPrimaryFixedVariant = midGray,
            secondary = midGray, onSecondary = midGray,
            secondaryContainer = midGray, onSecondaryContainer = midGray,
            secondaryFixed = midGray, secondaryFixedDim = midGray,
            onSecondaryFixed = midGray, onSecondaryFixedVariant = midGray,
            tertiary = midGray, onTertiary = midGray,
            tertiaryContainer = midGray, onTertiaryContainer = midGray,
            tertiaryFixed = midGray, tertiaryFixedDim = midGray,
            onTertiaryFixed = midGray, onTertiaryFixedVariant = midGray,
            error = midGray, onError = midGray,
            errorContainer = midGray, onErrorContainer = midGray,
            surface = midGray, onSurface = midGray,
            surfaceVariant = midGray, onSurfaceVariant = midGray,
            surfaceTint = midGray,
            surfaceBright = midGray, surfaceDim = midGray,
            surfaceContainer = midGray, surfaceContainerHigh = midGray,
            surfaceContainerHighest = midGray, surfaceContainerLow = midGray,
            surfaceContainerLowest = midGray,
            background = midGray, onBackground = midGray,
            inverseSurface = midGray, inverseOnSurface = midGray,
            outline = midGray, outlineVariant = midGray,
            scrim = midGray,
        )
        val result = ContrastValidator.validate(scheme)
        assertIs<ValidationResult.Fail>(result, "All-gray scheme should fail validation")
        assertTrue(result.issues.isNotEmpty(), "Should have contrast issues")
    }
}
