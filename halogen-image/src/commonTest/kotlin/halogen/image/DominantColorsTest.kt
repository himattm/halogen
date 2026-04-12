package halogen.image

import halogen.ThemeExpander
import halogen.color.Hct
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DominantColorsTest {

    private val hexPattern = Regex("^#[0-9A-Fa-f]{6}$")

    // ---- Helpers ----

    private fun colorFromHct(hue: Double, chroma: Double, tone: Double, population: Double): QuantizedColor {
        val argb = Hct.from(hue, chroma, tone).toInt()
        return QuantizedColor(argb = argb, population = population, hue = hue, chroma = chroma, tone = tone)
    }

    // ---- toSpec returns valid HalogenThemeSpec ----

    @Test
    fun toSpec_returnsValidSpec_allHexColorsParseable() {
        val c1 = colorFromHct(hue = 30.0, chroma = 80.0, tone = 50.0, population = 0.5)
        val c2 = colorFromHct(hue = 150.0, chroma = 40.0, tone = 60.0, population = 0.3)
        val c3 = colorFromHct(hue = 270.0, chroma = 25.0, tone = 45.0, population = 0.2)
        val colors = DominantColors(listOf(c1, c2, c3))
        val spec = colors.toSpec()

        // All hex fields should be parseable to ARGB
        val hexFields = listOf(spec.primary, spec.secondary, spec.tertiary, spec.neutralLight, spec.neutralDark, spec.error)
        for (hex in hexFields) {
            assertTrue(hexPattern.matches(hex), "Hex color should match #RRGGBB: $hex")
            // Should not throw
            val argb = parseHex(hex)
            assertNotNull(argb)
        }
    }

    // ---- toSpec survives ThemeExpander.expand() ----

    @Test
    fun toSpec_resultSurvivesThemeExpanderExpand() {
        val c1 = colorFromHct(hue = 200.0, chroma = 60.0, tone = 50.0, population = 0.6)
        val c2 = colorFromHct(hue = 80.0, chroma = 35.0, tone = 55.0, population = 0.4)
        val colors = DominantColors(listOf(c1, c2))
        val spec = colors.toSpec()

        // Should not throw
        val expanded = ThemeExpander.expand(spec)
        assertNotNull(expanded.lightColorScheme)
        assertNotNull(expanded.darkColorScheme)
        assertNotNull(expanded.typography)
        assertNotNull(expanded.shapes)
    }

    @Test
    fun toSpec_emptyColors_resultSurvivesExpand() {
        val spec = DominantColors(emptyList()).toSpec()
        val expanded = ThemeExpander.expand(spec)
        assertNotNull(expanded.lightColorScheme)
        assertNotNull(expanded.darkColorScheme)
    }

    // ---- toHint returns non-empty string with hex colors ----

    @Test
    fun toHint_nonEmpty_containsHexColors() {
        val c1 = colorFromHct(hue = 30.0, chroma = 80.0, tone = 50.0, population = 0.6)
        val c2 = colorFromHct(hue = 150.0, chroma = 40.0, tone = 60.0, population = 0.4)
        val colors = DominantColors(listOf(c1, c2))
        val hint = colors.toHint()

        assertTrue(hint.isNotEmpty(), "toHint should return non-empty string")
        assertTrue(hint.contains("#"), "toHint should contain hex color references")
    }

    @Test
    fun toHint_emptyColors_returnsNonEmptyString() {
        val hint = DominantColors(emptyList()).toHint()
        assertTrue(hint.isNotEmpty(), "toHint on empty colors should return non-empty fallback string")
    }

    // ---- toHint includes mood descriptor ----

    @Test
    fun toHint_includesMoodDescriptor() {
        // Dark, vibrant colors
        val c1 = colorFromHct(hue = 0.0, chroma = 80.0, tone = 30.0, population = 0.7)
        val c2 = colorFromHct(hue = 240.0, chroma = 60.0, tone = 25.0, population = 0.3)
        val colors = DominantColors(listOf(c1, c2))
        val hint = colors.toHint()

        // Should contain a mood descriptor like "dark, vibrant" or "light, subdued"
        assertTrue(hint.contains("mood", ignoreCase = true), "toHint should include mood descriptor")
    }

    @Test
    fun toHint_darkVibrantColors_includesDarkMood() {
        val c1 = colorFromHct(hue = 0.0, chroma = 80.0, tone = 20.0, population = 1.0)
        val colors = DominantColors(listOf(c1))
        val hint = colors.toHint()

        assertTrue(hint.contains("dark"), "Dark-toned colors should produce 'dark' mood descriptor")
        assertTrue(hint.contains("vibrant"), "High-chroma colors should produce 'vibrant' mood descriptor")
    }

    @Test
    fun toHint_lightSubduedColors_includesLightMood() {
        val c1 = colorFromHct(hue = 200.0, chroma = 10.0, tone = 80.0, population = 1.0)
        val colors = DominantColors(listOf(c1))
        val hint = colors.toHint()

        assertTrue(hint.contains("light"), "Light-toned colors should produce 'light' mood descriptor")
        assertTrue(hint.contains("subdued"), "Low-chroma colors should produce 'subdued' mood descriptor")
    }

    // ---- toHint includes inline few-shot example ----

    @Test
    fun toHint_includesInlineFewShotExample() {
        val c1 = colorFromHct(hue = 30.0, chroma = 80.0, tone = 50.0, population = 1.0)
        val colors = DominantColors(listOf(c1))
        val hint = colors.toHint()

        // The hint should contain the example palette and JSON
        assertTrue(hint.contains("#1B4332"), "toHint should include example palette hex")
        assertTrue(hint.contains("\"pri\""), "toHint should include example JSON keys")
        assertTrue(hint.contains("For example"), "toHint should include few-shot example intro")
    }

    // ---- Round-trip: synthetic pixels -> extract -> toSpec -> expand ----

    @Test
    fun roundTrip_syntheticPixels_extractToSpecExpand_producesValidScheme() {
        // Create a simple two-color image
        val red = 0xFFFF0000.toInt()
        val blue = 0xFF0000FF.toInt()
        val pixels = IntArray(200) { if (it < 120) red else blue }

        val dominantColors = ImageQuantizer.extract(pixels, 200, 1)
        val spec = dominantColors.toSpec()

        // All hex values should be valid
        val hexFields = listOf(spec.primary, spec.secondary, spec.tertiary, spec.neutralLight, spec.neutralDark, spec.error)
        for (hex in hexFields) {
            assertTrue(hexPattern.matches(hex), "Hex should match #RRGGBB: $hex")
        }

        // Should expand without error
        val expanded = ThemeExpander.expand(spec)
        assertNotNull(expanded.lightColorScheme, "Light scheme should not be null")
        assertNotNull(expanded.darkColorScheme, "Dark scheme should not be null")
        assertNotNull(expanded.typography, "Typography should not be null")
        assertNotNull(expanded.shapes, "Shapes should not be null")

        // Light and dark should produce different surfaces
        assertTrue(
            expanded.lightColorScheme.surface != expanded.darkColorScheme.surface,
            "Light and dark surfaces should differ",
        )
    }

    // ---- toString ----

    @Test
    fun toString_showsColorCount() {
        val c1 = colorFromHct(hue = 30.0, chroma = 80.0, tone = 50.0, population = 1.0)
        val colors = DominantColors(listOf(c1))
        assertTrue(colors.toString().contains("1 colors"), "toString should show color count")
    }

    @Test
    fun toString_emptyColors_showsZero() {
        val colors = DominantColors(emptyList())
        assertTrue(colors.toString().contains("0 colors"), "toString on empty should show 0 colors")
    }
}
