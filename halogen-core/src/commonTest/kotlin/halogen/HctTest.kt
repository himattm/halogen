package halogen

import halogen.color.Hct
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HctTest {

    /**
     * Helper: checks that every channel of two ARGB ints differs by at most [tolerance].
     */
    private fun assertArgbClose(expected: Int, actual: Int, tolerance: Int = 1) {
        val eA = (expected shr 24) and 0xFF
        val eR = (expected shr 16) and 0xFF
        val eG = (expected shr 8) and 0xFF
        val eB = expected and 0xFF

        val aA = (actual shr 24) and 0xFF
        val aR = (actual shr 16) and 0xFF
        val aG = (actual shr 8) and 0xFF
        val aB = actual and 0xFF

        assertTrue(
            abs(eA - aA) <= tolerance &&
                abs(eR - aR) <= tolerance &&
                abs(eG - aG) <= tolerance &&
                abs(eB - aB) <= tolerance,
            "ARGB mismatch: expected 0x${expected.toUInt().toString(16).padStart(8, '0')}, " +
                "got 0x${actual.toUInt().toString(16).padStart(8, '0')} (tolerance=$tolerance)",
        )
    }

    // ---- ARGB -> HCT -> ARGB round-trip identity ----

    @Test
    fun roundTrip_pureRed() {
        val argb = 0xFFFF0000.toInt()
        val hct = Hct.fromInt(argb)
        assertArgbClose(argb, hct.toInt())
    }

    @Test
    fun roundTrip_pureGreen() {
        val argb = 0xFF00FF00.toInt()
        val hct = Hct.fromInt(argb)
        assertArgbClose(argb, hct.toInt())
    }

    @Test
    fun roundTrip_pureBlue() {
        val argb = 0xFF0000FF.toInt()
        val hct = Hct.fromInt(argb)
        assertArgbClose(argb, hct.toInt())
    }

    @Test
    fun roundTrip_white() {
        val argb = 0xFFFFFFFF.toInt()
        val hct = Hct.fromInt(argb)
        assertArgbClose(argb, hct.toInt())
    }

    @Test
    fun roundTrip_black() {
        val argb = 0xFF000000.toInt()
        val hct = Hct.fromInt(argb)
        assertArgbClose(argb, hct.toInt())
    }

    @Test
    fun roundTrip_midGray() {
        val argb = 0xFF808080.toInt()
        val hct = Hct.fromInt(argb)
        assertArgbClose(argb, hct.toInt())
    }

    @Test
    fun roundTrip_googleBlue() {
        val argb = 0xFF4285F4.toInt()
        val hct = Hct.fromInt(argb)
        assertArgbClose(argb, hct.toInt())
    }

    // ---- from(h,c,t) -> toInt() -> fromInt() preserves HCT values ----

    @Test
    fun fromHctRoundTrip_preservesValues() {
        val original = Hct.from(120.0, 50.0, 60.0)
        val restored = Hct.fromInt(original.toInt())

        assertTrue(
            abs(original.hue - restored.hue) < 2.0,
            "Hue diverged: ${original.hue} vs ${restored.hue}",
        )
        assertTrue(
            abs(original.chroma - restored.chroma) < 2.0,
            "Chroma diverged: ${original.chroma} vs ${restored.chroma}",
        )
        assertTrue(
            abs(original.tone - restored.tone) < 2.0,
            "Tone diverged: ${original.tone} vs ${restored.tone}",
        )
    }

    @Test
    fun fromHctRoundTrip_variousHues() {
        val hues = listOf(0.0, 30.0, 60.0, 120.0, 180.0, 240.0, 300.0, 350.0)
        for (hue in hues) {
            val original = Hct.from(hue, 40.0, 50.0)
            val restored = Hct.fromInt(original.toInt())
            assertTrue(
                abs(original.tone - restored.tone) < 2.0,
                "Tone diverged at hue $hue: ${original.tone} vs ${restored.tone}",
            )
        }
    }

    // ---- Tone boundary conditions ----

    @Test
    fun tone0_isBlack() {
        val hct = Hct.from(0.0, 0.0, 0.0)
        val argb = hct.toInt()
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        assertTrue(r <= 1 && g <= 1 && b <= 1, "Tone 0 should be black, got r=$r g=$g b=$b")
    }

    @Test
    fun tone100_isWhite() {
        val hct = Hct.from(0.0, 0.0, 100.0)
        val argb = hct.toInt()
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        assertTrue(r >= 254 && g >= 254 && b >= 254, "Tone 100 should be white, got r=$r g=$g b=$b")
    }

    // ---- Chroma 0 is grayscale ----

    @Test
    fun chroma0_isGrayscale() {
        for (tone in listOf(10.0, 30.0, 50.0, 70.0, 90.0)) {
            val hct = Hct.from(200.0, 0.0, tone)
            val argb = hct.toInt()
            val r = (argb shr 16) and 0xFF
            val g = (argb shr 8) and 0xFF
            val b = argb and 0xFF
            // Grayscale means R == G == B (within 1 due to rounding)
            assertTrue(
                abs(r - g) <= 1 && abs(g - b) <= 1,
                "Chroma 0 at tone $tone should be grayscale, got r=$r g=$g b=$b",
            )
        }
    }
}
