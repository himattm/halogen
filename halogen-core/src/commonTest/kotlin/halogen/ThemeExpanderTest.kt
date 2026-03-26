package halogen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ThemeExpanderTest {

    private val defaultSpec = HalogenThemeSpec(
        primary = "#6750A4",
        secondary = "#625B71",
        tertiary = "#7D5260",
        neutralLight = "#FFFBFE",
        neutralDark = "#1C1B1F",
        error = "#B3261E",
        fontMood = "modern",
        headingWeight = 700,
        bodyWeight = 400,
        tightLetterSpacing = false,
        cornerStyle = "rounded",
        cornerScale = 1.0f,
    )

    // ---- Full expansion ----

    @Test
    fun expand_producesCompleteTheme() {
        val theme = ThemeExpander.expand(defaultSpec)
        assertNotNull(theme.palette)
        assertNotNull(theme.lightColorScheme)
        assertNotNull(theme.darkColorScheme)
        assertNotNull(theme.typography)
        assertNotNull(theme.shapes)
    }

    @Test
    fun lightAndDark_haveDifferentSurfaces() {
        val theme = ThemeExpander.expand(defaultSpec)
        assertNotEquals(
            theme.lightColorScheme.surface,
            theme.darkColorScheme.surface,
            "Light and dark surface colors should differ",
        )
    }

    @Test
    fun lightAndDark_haveDifferentPrimary() {
        val theme = ThemeExpander.expand(defaultSpec)
        assertNotEquals(
            theme.lightColorScheme.primary,
            theme.darkColorScheme.primary,
            "Light and dark primary colors should differ",
        )
    }

    // ---- Typography expansion ----

    @Test
    fun expandTypography_preservesFontMood() {
        val typography = ThemeExpander.expandTypography(defaultSpec)
        assertEquals("modern", typography.fontMood)
    }

    @Test
    fun expandTypography_preservesWeights() {
        val typography = ThemeExpander.expandTypography(defaultSpec)
        assertEquals(700, typography.headingWeight)
        assertEquals(400, typography.bodyWeight)
    }

    @Test
    fun expandTypography_clampsWeights() {
        val spec = defaultSpec.copy(headingWeight = 1000, bodyWeight = 50)
        val typography = ThemeExpander.expandTypography(spec)
        assertEquals(900, typography.headingWeight, "Heading weight should clamp to 900")
        assertEquals(100, typography.bodyWeight, "Body weight should clamp to 100")
    }

    @Test
    fun expandTypography_preservesLetterSpacing() {
        val typography = ThemeExpander.expandTypography(defaultSpec)
        assertEquals(false, typography.tightLetterSpacing)
    }

    // ---- Shapes expansion ----

    @Test
    fun expandShapes_roundedDefault() {
        val shapes = ThemeExpander.expandShapes(defaultSpec)
        assertEquals(4f, shapes.extraSmall)
        assertEquals(8f, shapes.small)
        assertEquals(16f, shapes.medium)
        assertEquals(24f, shapes.large)
        assertEquals(32f, shapes.extraLarge)
    }

    @Test
    fun expandShapes_sharpStyle() {
        val spec = defaultSpec.copy(cornerStyle = "sharp", cornerScale = 1.0f)
        val shapes = ThemeExpander.expandShapes(spec)
        assertEquals(0f, shapes.extraSmall)
        assertEquals(2f, shapes.small)
    }

    // ---- Hex parsing ----

    @Test
    fun parseHex_uppercaseValid() {
        val argb = ThemeExpander.parseHexToArgb("#1A73E8")
        // 0xFF1A73E8
        val expected = 0xFF1A73E8.toInt()
        assertEquals(expected, argb)
    }

    @Test
    fun parseHex_lowercaseValid() {
        val argb = ThemeExpander.parseHexToArgb("#1a73e8")
        val expected = 0xFF1A73E8.toInt()
        assertEquals(expected, argb)
    }

    @Test
    fun parseHex_invalidHex_throws() {
        assertFailsWith<IllegalArgumentException> {
            ThemeExpander.parseHexToArgb("1A73E8") // Missing #
        }
    }

    @Test
    fun parseHex_tooShort_throws() {
        assertFailsWith<IllegalArgumentException> {
            ThemeExpander.parseHexToArgb("#FFF") // 3-digit shorthand not supported
        }
    }

    @Test
    fun parseHex_tooLong_throws() {
        assertFailsWith<IllegalArgumentException> {
            ThemeExpander.parseHexToArgb("#FF1A73E8") // 8-digit not supported
        }
    }

    @Test
    fun parseHex_black() {
        val argb = ThemeExpander.parseHexToArgb("#000000")
        assertEquals(0xFF000000.toInt(), argb)
    }

    @Test
    fun parseHex_white() {
        val argb = ThemeExpander.parseHexToArgb("#FFFFFF")
        assertEquals(0xFFFFFFFF.toInt(), argb)
    }

    // ---- expandColors helper ----

    @Test
    fun expandColors_lightMode() {
        val lightScheme = ThemeExpander.expandColors(defaultSpec, isDark = false)
        val fullTheme = ThemeExpander.expand(defaultSpec)
        assertEquals(fullTheme.lightColorScheme, lightScheme, "expandColors(light) should match full expand")
    }

    @Test
    fun expandColors_darkMode() {
        val darkScheme = ThemeExpander.expandColors(defaultSpec, isDark = true)
        val fullTheme = ThemeExpander.expand(defaultSpec)
        assertEquals(fullTheme.darkColorScheme, darkScheme, "expandColors(dark) should match full expand")
    }
}
