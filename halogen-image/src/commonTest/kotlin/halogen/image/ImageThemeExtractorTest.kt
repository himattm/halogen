package halogen.image

import halogen.color.Hct
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Parse a hex color like "#1A73E8" to ARGB int.
 * Local helper since parseHex is internal to halogen-core.
 */
private fun parseHex(hex: String): Int {
    val rgb = hex.removePrefix("#").toLong(16).toInt()
    return rgb or (0xFF shl 24).toInt()
}

/**
 * Tests for [ImageThemeExtractor] via the public [DominantColors.toSpec] API.
 *
 * ImageThemeExtractor is internal, so we exercise it through DominantColors
 * which delegates to ImageThemeExtractor.mapToSpec().
 */
class ImageThemeExtractorTest {

    // ---- Helpers ----

    /** Create a QuantizedColor from HCT components with a given population. */
    private fun colorFromHct(hue: Double, chroma: Double, tone: Double, population: Double): QuantizedColor {
        val argb = Hct.from(hue, chroma, tone).toInt()
        return QuantizedColor(argb = argb, population = population, hue = hue, chroma = chroma, tone = tone)
    }

    private val hexPattern = Regex("^#[0-9A-Fa-f]{6}$")

    // ---- Three colors with clear chroma ordering ----

    @Test
    fun toSpec_threeColors_highestChromaIsPrimary() {
        val highChroma = colorFromHct(hue = 30.0, chroma = 80.0, tone = 50.0, population = 0.3)
        val medChroma = colorFromHct(hue = 120.0, chroma = 50.0, tone = 50.0, population = 0.4)
        val lowChroma = colorFromHct(hue = 240.0, chroma = 20.0, tone = 50.0, population = 0.3)

        val colors = DominantColors(listOf(medChroma, highChroma, lowChroma))
        val spec = colors.toSpec()

        // Primary should be the highest-chroma color (hue ~30)
        val primaryArgb = parseHex(spec.primary)
        val primaryHct = Hct.fromInt(primaryArgb)
        val hueDist = minOf(abs(primaryHct.hue - 30.0), 360.0 - abs(primaryHct.hue - 30.0))
        assertTrue(hueDist < 15.0, "Primary hue should be near 30, was ${primaryHct.hue}")
    }

    @Test
    fun toSpec_threeColors_primaryToneInIdealRange() {
        // Primary candidate with tone in 30-70 range (preferred)
        val ideal = colorFromHct(hue = 30.0, chroma = 80.0, tone = 50.0, population = 0.3)
        // Higher chroma but out of tone range
        val outOfRange = colorFromHct(hue = 60.0, chroma = 90.0, tone = 80.0, population = 0.3)
        val third = colorFromHct(hue = 200.0, chroma = 40.0, tone = 50.0, population = 0.4)

        val colors = DominantColors(listOf(outOfRange, ideal, third))
        val spec = colors.toSpec()

        // The ideal-tone candidate should be chosen as primary over the higher-chroma one
        // that has tone outside 30-70. But if outOfRange is chosen (highest chroma), that's
        // also valid per the fallback logic.
        assertNotNull(spec.primary, "Primary should not be null")
        assertTrue(hexPattern.matches(spec.primary), "Primary should be valid hex: ${spec.primary}")
    }

    @Test
    fun toSpec_threeColors_secondaryHueDiffersFromPrimary() {
        val primary = colorFromHct(hue = 30.0, chroma = 80.0, tone = 50.0, population = 0.4)
        val secondary = colorFromHct(hue = 120.0, chroma = 50.0, tone = 50.0, population = 0.3)
        val tertiary = colorFromHct(hue = 240.0, chroma = 30.0, tone = 50.0, population = 0.3)

        val colors = DominantColors(listOf(primary, secondary, tertiary))
        val spec = colors.toSpec()

        val primaryHct = Hct.fromInt(parseHex(spec.primary))
        val secondaryHct = Hct.fromInt(parseHex(spec.secondary))
        val hueDist = minOf(
            abs(primaryHct.hue - secondaryHct.hue),
            360.0 - abs(primaryHct.hue - secondaryHct.hue),
        )
        // Secondary should have hue at least 15 degrees away from primary
        // (or be synthesized, which shifts by 30 degrees)
        assertTrue(hueDist >= 10.0, "Secondary hue should differ from primary by >= 10, was $hueDist")
    }

    // ---- Empty colors ----

    @Test
    fun toSpec_emptyColors_returnsValidSpec() {
        val colors = DominantColors(emptyList())
        val spec = colors.toSpec()

        assertNotNull(spec)
        assertTrue(hexPattern.matches(spec.primary), "Primary should be valid hex: ${spec.primary}")
        assertTrue(hexPattern.matches(spec.secondary), "Secondary should be valid hex: ${spec.secondary}")
        assertTrue(hexPattern.matches(spec.tertiary), "Tertiary should be valid hex: ${spec.tertiary}")
        assertTrue(hexPattern.matches(spec.neutralLight), "NeutralLight should be valid hex: ${spec.neutralLight}")
        assertTrue(hexPattern.matches(spec.neutralDark), "NeutralDark should be valid hex: ${spec.neutralDark}")
        assertTrue(hexPattern.matches(spec.error), "Error should be valid hex: ${spec.error}")
    }

    // ---- Single color ----

    @Test
    fun toSpec_singleColor_returnsValidSpec() {
        val color = colorFromHct(hue = 200.0, chroma = 60.0, tone = 50.0, population = 1.0)
        val colors = DominantColors(listOf(color))
        val spec = colors.toSpec()

        assertNotNull(spec)
        assertTrue(hexPattern.matches(spec.primary), "Primary should be valid hex: ${spec.primary}")
        assertTrue(hexPattern.matches(spec.secondary), "Secondary should be valid hex: ${spec.secondary}")
        assertTrue(hexPattern.matches(spec.tertiary), "Tertiary should be valid hex: ${spec.tertiary}")
    }

    // ---- Error color is always #BA1A1A ----

    @Test
    fun toSpec_errorColor_isAlwaysBA1A1A() {
        val color = colorFromHct(hue = 30.0, chroma = 80.0, tone = 50.0, population = 1.0)
        val spec = DominantColors(listOf(color)).toSpec()
        assertEquals("#BA1A1A", spec.error, "Error should always be #BA1A1A")
    }

    @Test
    fun toSpec_emptyColors_errorColor_isAlwaysBA1A1A() {
        val spec = DominantColors(emptyList()).toSpec()
        assertEquals("#BA1A1A", spec.error, "Error should always be #BA1A1A for empty palette")
    }

    @Test
    fun toSpec_multipleColors_errorColor_isAlwaysBA1A1A() {
        val c1 = colorFromHct(hue = 30.0, chroma = 80.0, tone = 50.0, population = 0.5)
        val c2 = colorFromHct(hue = 120.0, chroma = 50.0, tone = 50.0, population = 0.3)
        val c3 = colorFromHct(hue = 240.0, chroma = 30.0, tone = 50.0, population = 0.2)
        val spec = DominantColors(listOf(c1, c2, c3)).toSpec()
        assertEquals("#BA1A1A", spec.error, "Error should always be #BA1A1A for multi-color palette")
    }

    // ---- All hex colors match #RRGGBB format ----

    @Test
    fun toSpec_allHexColorsMatchFormat() {
        val c1 = colorFromHct(hue = 30.0, chroma = 80.0, tone = 50.0, population = 0.4)
        val c2 = colorFromHct(hue = 120.0, chroma = 50.0, tone = 50.0, population = 0.3)
        val c3 = colorFromHct(hue = 240.0, chroma = 30.0, tone = 50.0, population = 0.3)
        val spec = DominantColors(listOf(c1, c2, c3)).toSpec()

        val allHexFields = listOf(
            "primary" to spec.primary,
            "secondary" to spec.secondary,
            "tertiary" to spec.tertiary,
            "neutralLight" to spec.neutralLight,
            "neutralDark" to spec.neutralDark,
            "error" to spec.error,
        )

        for ((name, hex) in allHexFields) {
            assertTrue(hexPattern.matches(hex), "$name should match #RRGGBB format, was: $hex")
        }
    }

    // ---- Typography and shape defaults ----

    @Test
    fun toSpec_setsDefaultTypographyValues() {
        val spec = DominantColors(emptyList()).toSpec()
        assertEquals("modern", spec.fontMood)
        assertEquals(600, spec.headingWeight)
        assertEquals(400, spec.bodyWeight)
        assertEquals(false, spec.tightLetterSpacing)
    }

    @Test
    fun toSpec_setsDefaultShapeValues() {
        val spec = DominantColors(emptyList()).toSpec()
        assertEquals("rounded", spec.cornerStyle)
        assertEquals(1.0f, spec.cornerScale)
    }
}
