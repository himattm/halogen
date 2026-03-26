package halogen

import halogen.color.Hct
import halogen.color.TonalPalette
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TonalPaletteTest {

    private val standardTones = listOf(0, 5, 10, 20, 30, 40, 50, 60, 70, 80, 90, 95, 100)

    // Google Blue seed
    private val seedArgb = 0xFF4285F4.toInt()

    @Test
    fun allStandardTones_produceValidArgb() {
        val palette = TonalPalette.fromInt(seedArgb)
        for (tone in standardTones) {
            val argb = palette.tone(tone)
            // Valid ARGB has full alpha
            val alpha = (argb.toLong() and 0xFF000000L) ushr 24
            assertEquals(255L, alpha, "Tone $tone should have full alpha, got $alpha")
        }
    }

    @Test
    fun huePreservation_acrossTones() {
        val palette = TonalPalette.fromInt(seedArgb)
        val referenceHue = palette.hue

        // Check mid-range tones where chroma is high enough for meaningful hue.
        // Very dark (< 20) or very light (> 90) tones may become achromatic,
        // causing hue to collapse to 0.
        for (tone in listOf(30, 40, 50, 60, 70, 80)) {
            val hct = Hct.fromInt(palette.tone(tone))
            // Skip near-achromatic results (chroma < 2) since hue is meaningless
            if (hct.chroma < 2.0) continue
            // Hue can wrap around 360, so compute circular difference
            val diff = hueDifference(referenceHue, hct.hue)
            assertTrue(
                diff <= 4.0,
                "Hue at tone $tone (${hct.hue}) diverged from palette hue ($referenceHue) by $diff degrees",
            )
        }
    }

    @Test
    fun caching_returnsSameResult() {
        val palette = TonalPalette.fromInt(seedArgb)
        val first = palette.tone(40)
        val second = palette.tone(40)
        assertEquals(first, second, "Repeated tone(40) calls should return the same ARGB value")
    }

    @Test
    fun fromHueAndChroma_producesValidPalette() {
        val palette = TonalPalette.fromHueAndChroma(270.0, 36.0)
        for (tone in standardTones) {
            val argb = palette.tone(tone)
            assertNotEquals(0, argb, "Tone $tone should produce a non-zero ARGB value")
        }
    }

    @Test
    fun fromHct_preservesHueAndChroma() {
        val hct = Hct.fromInt(seedArgb)
        val palette = TonalPalette.fromHct(hct)
        assertEquals(hct.hue, palette.hue, "Palette hue should equal HCT hue")
        assertEquals(hct.chroma, palette.chroma, "Palette chroma should equal HCT chroma")
    }

    @Test
    fun differentSeeds_produceDifferentPalettes() {
        val palette1 = TonalPalette.fromInt(0xFFFF0000.toInt()) // Red
        val palette2 = TonalPalette.fromInt(0xFF0000FF.toInt()) // Blue
        assertNotEquals(
            palette1.tone(50),
            palette2.tone(50),
            "Different seed colors should produce different palettes at tone 50",
        )
    }

    /** Compute the shortest angular distance between two hues in degrees. */
    private fun hueDifference(a: Double, b: Double): Double {
        return 180.0 - abs(abs(a - b) - 180.0)
    }
}
