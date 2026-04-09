package halogen.image

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ImageQuantizerTest {

    // ---- Helpers ----

    private fun solidPixels(color: Int, count: Int): Triple<IntArray, Int, Int> {
        return Triple(IntArray(count) { color }, count, 1)
    }

    private fun twoColorPixels(c1: Int, count1: Int, c2: Int, count2: Int): Triple<IntArray, Int, Int> {
        val total = count1 + count2
        val pixels = IntArray(total) { if (it < count1) c1 else c2 }
        return Triple(pixels, total, 1)
    }

    // ---- Single-color image ----

    @Test
    fun extract_singleRedColor_returnsOneColorWithRedHue() {
        val (pixels, w, h) = solidPixels(0xFFFF0000.toInt(), 100)
        val result = ImageQuantizer.extract(pixels, w, h)
        assertEquals(1, result.colors.size, "Should extract exactly 1 color from single-color image")
        val color = result.colors[0]
        // Red hue in HCT is near 27 degrees (not exactly 0 due to CAM16 hue mapping)
        assertTrue(color.hue < 40.0 || color.hue > 340.0, "Hue should be in red range, was ${color.hue}")
        assertTrue(color.chroma > 0.0, "Chroma should be positive for red")
    }

    // ---- Two-color image ----

    @Test
    fun extract_twoDistinctColors_returnsTwoColorsWithDistinctHues() {
        val (pixels, w, h) = twoColorPixels(
            0xFFFF0000.toInt(), 50, // red
            0xFF0000FF.toInt(), 50, // blue
        )
        val result = ImageQuantizer.extract(pixels, w, h)
        assertEquals(2, result.colors.size, "Should extract 2 colors from two-color image")
        val hue1 = result.colors[0].hue
        val hue2 = result.colors[1].hue
        val hueDist = minOf(abs(hue1 - hue2), 360.0 - abs(hue1 - hue2))
        assertTrue(hueDist > 30.0, "Hues should be distinct, distance was $hueDist")
    }

    // ---- Achromatic images with default chroma filter ----

    @Test
    fun extract_allBlack_returnsEmptyColors() {
        val (pixels, w, h) = solidPixels(0xFF000000.toInt(), 100)
        val result = ImageQuantizer.extract(pixels, w, h)
        assertTrue(result.colors.isEmpty(), "All-black image should yield empty colors (chroma filter)")
    }

    @Test
    fun extract_allWhite_returnsEmptyColors() {
        val (pixels, w, h) = solidPixels(0xFFFFFFFF.toInt(), 100)
        val result = ImageQuantizer.extract(pixels, w, h)
        assertTrue(result.colors.isEmpty(), "All-white image should yield empty colors (chroma filter)")
    }

    // ---- minChroma = 0.0 preserves achromatic ----

    @Test
    fun extract_allBlackWithZeroMinChroma_preservesAchromaticColor() {
        val (pixels, w, h) = solidPixels(0xFF000000.toInt(), 100)
        val result = ImageQuantizer.extract(pixels, w, h, minChroma = 0.0)
        assertTrue(result.colors.isNotEmpty(), "With minChroma=0.0, achromatic colors should be preserved")
    }

    @Test
    fun extract_allWhiteWithZeroMinChroma_preservesAchromaticColor() {
        val (pixels, w, h) = solidPixels(0xFFFFFFFF.toInt(), 100)
        val result = ImageQuantizer.extract(pixels, w, h, minChroma = 0.0)
        assertTrue(result.colors.isNotEmpty(), "With minChroma=0.0, achromatic colors should be preserved")
    }

    // ---- Transparency handling ----

    @Test
    fun extract_transparentPixels_areSkipped() {
        // Transparent red (alpha = 0)
        val (pixels, w, h) = solidPixels(0x00FF0000, 100)
        val result = ImageQuantizer.extract(pixels, w, h)
        assertTrue(result.colors.isEmpty(), "Fully transparent pixels should be skipped")
    }

    @Test
    fun extract_semiTransparentPixels_belowThreshold_areSkipped() {
        // Alpha = 127 (below 128 threshold)
        val (pixels, w, h) = solidPixels(0x7FFF0000.toInt(), 100)
        val result = ImageQuantizer.extract(pixels, w, h)
        assertTrue(result.colors.isEmpty(), "Pixels with alpha < 128 should be skipped")
    }

    @Test
    fun extract_semiTransparentPixels_atThreshold_areKept() {
        // Alpha = 128 (at threshold)
        val pixel = (128 shl 24) or 0xFF0000 // 0x80FF0000
        val (pixels, w, h) = solidPixels(pixel, 100)
        val result = ImageQuantizer.extract(pixels, w, h)
        assertTrue(result.colors.isNotEmpty(), "Pixels with alpha = 128 should be kept")
    }

    @Test
    fun extract_fullyTransparentImage_returnsEmptyResult() {
        val (pixels, w, h) = solidPixels(0x00000000, 100)
        val result = ImageQuantizer.extract(pixels, w, h)
        assertTrue(result.colors.isEmpty(), "Fully transparent image should yield empty result")
    }

    // ---- maxColors parameter ----

    @Test
    fun extract_maxColors_limitsOutput() {
        // Create multi-color image: red, green, blue pixels
        val red = 0xFFFF0000.toInt()
        val green = 0xFF00FF00.toInt()
        val blue = 0xFF0000FF.toInt()
        val pixels = IntArray(300) { i ->
            when {
                i < 100 -> red
                i < 200 -> green
                else -> blue
            }
        }
        val result = ImageQuantizer.extract(pixels, 300, 1, maxColors = 3)
        assertTrue(result.colors.size <= 3, "Should return at most 3 colors, got ${result.colors.size}")
    }

    @Test
    fun extract_maxColors1_returnsExactlyOneColor() {
        val (pixels, w, h) = twoColorPixels(
            0xFFFF0000.toInt(), 50,
            0xFF0000FF.toInt(), 50,
        )
        val result = ImageQuantizer.extract(pixels, w, h, maxColors = 1)
        assertEquals(1, result.colors.size, "maxColors=1 should return exactly 1 color")
    }

    // ---- Edge case: empty/invalid dimensions ----

    @Test
    fun extract_zeroDimensions_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            ImageQuantizer.extract(IntArray(0), 0, 0)
        }
    }

    @Test
    fun extract_zeroWidth_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            ImageQuantizer.extract(IntArray(0), 0, 1)
        }
    }

    @Test
    fun extract_zeroHeight_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            ImageQuantizer.extract(IntArray(0), 1, 0)
        }
    }

    @Test
    fun extract_pixelArrayTooSmall_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            ImageQuantizer.extract(IntArray(5), 10, 10)
        }
    }

    // ---- Population fractions ----

    @Test
    fun extract_populationFractions_sumToApproximatelyOne() {
        val (pixels, w, h) = twoColorPixels(
            0xFFFF0000.toInt(), 70,
            0xFF0000FF.toInt(), 30,
        )
        val result = ImageQuantizer.extract(pixels, w, h)
        if (result.colors.isNotEmpty()) {
            val totalPopulation = result.colors.sumOf { it.population }
            assertTrue(
                abs(totalPopulation - 1.0) < 0.05,
                "Population fractions should sum to ~1.0, got $totalPopulation",
            )
        }
    }

    // ---- Results are sorted by population descending ----

    @Test
    fun extract_resultsSortedByPopulationDescending() {
        val (pixels, w, h) = twoColorPixels(
            0xFFFF0000.toInt(), 70, // majority red
            0xFF0000FF.toInt(), 30, // minority blue
        )
        val result = ImageQuantizer.extract(pixels, w, h)
        if (result.colors.size >= 2) {
            assertTrue(
                result.colors[0].population >= result.colors[1].population,
                "Colors should be sorted by population descending",
            )
        }
    }
}
