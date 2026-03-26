package halogen

import halogen.color.Hct
import halogen.color.TonalPalette
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DynamicSchemeTest {

    // Material 3 default seed colors
    private val primaryPalette = TonalPalette.fromInt(0xFF6750A4.toInt())
    private val secondaryPalette = TonalPalette.fromInt(0xFF625B71.toInt())
    private val tertiaryPalette = TonalPalette.fromInt(0xFF7D5260.toInt())
    private val neutralPalette = TonalPalette.fromHueAndChroma(270.0, 6.0)
    private val neutralVariantPalette = TonalPalette.fromHueAndChroma(270.0, 8.0)
    private val errorPalette = TonalPalette.fromHueAndChroma(25.0, 84.0)

    private val lightScheme = DynamicScheme.buildColorScheme(
        isDark = false,
        primaryPalette = primaryPalette,
        secondaryPalette = secondaryPalette,
        tertiaryPalette = tertiaryPalette,
        neutralPalette = neutralPalette,
        neutralVariantPalette = neutralVariantPalette,
        errorPalette = errorPalette,
    )

    private val darkScheme = DynamicScheme.buildColorScheme(
        isDark = true,
        primaryPalette = primaryPalette,
        secondaryPalette = secondaryPalette,
        tertiaryPalette = tertiaryPalette,
        neutralPalette = neutralPalette,
        neutralVariantPalette = neutralVariantPalette,
        errorPalette = errorPalette,
    )

    @Test
    fun lightScheme_primaryMapsTone40() {
        val expected = primaryPalette.tone(40)
        assertEquals(expected, lightScheme.primary, "Light primary should map to tone 40")
    }

    @Test
    fun lightScheme_surfaceMapsTone98() {
        val expected = neutralPalette.tone(98)
        assertEquals(expected, lightScheme.surface, "Light surface should map to tone 98")
    }

    @Test
    fun darkScheme_primaryMapsTone80() {
        val expected = primaryPalette.tone(80)
        assertEquals(expected, darkScheme.primary, "Dark primary should map to tone 80")
    }

    @Test
    fun darkScheme_surfaceMapsTone6() {
        val expected = neutralPalette.tone(6)
        assertEquals(expected, darkScheme.surface, "Dark surface should map to tone 6")
    }

    @Test
    fun allRoles_produceNonZeroArgb_light() {
        val roles = collectAllRoles(lightScheme)
        assertEquals(48, roles.size, "Should have 48 color roles")
        for ((name, value) in roles) {
            // 0xFF000000 (black) is a valid ARGB, so just check alpha is set
            val alpha = (value.toLong() and 0xFF000000L) ushr 24
            assertEquals(255L, alpha, "Light role $name should have full alpha")
        }
    }

    @Test
    fun allRoles_produceNonZeroArgb_dark() {
        val roles = collectAllRoles(darkScheme)
        assertEquals(48, roles.size, "Should have 48 color roles")
        for ((name, value) in roles) {
            val alpha = (value.toLong() and 0xFF000000L) ushr 24
            assertEquals(255L, alpha, "Dark role $name should have full alpha")
        }
    }

    @Test
    fun lightAndDark_produceDifferentPrimary() {
        assertNotEquals(
            lightScheme.primary,
            darkScheme.primary,
            "Light and dark primary should differ",
        )
    }

    @Test
    fun lightAndDark_produceDifferentSurface() {
        assertNotEquals(
            lightScheme.surface,
            darkScheme.surface,
            "Light and dark surface should differ",
        )
    }

    @Test
    fun lightScheme_onPrimaryIsTone100() {
        assertEquals(
            primaryPalette.tone(100),
            lightScheme.onPrimary,
            "Light onPrimary should be tone 100",
        )
    }

    @Test
    fun darkScheme_onPrimaryIsTone20() {
        assertEquals(
            primaryPalette.tone(20),
            darkScheme.onPrimary,
            "Dark onPrimary should be tone 20",
        )
    }

    /** Collect all 48 color roles into a map for iteration. */
    private fun collectAllRoles(scheme: HalogenColorScheme): Map<String, Int> = mapOf(
        "primary" to scheme.primary,
        "onPrimary" to scheme.onPrimary,
        "primaryContainer" to scheme.primaryContainer,
        "onPrimaryContainer" to scheme.onPrimaryContainer,
        "inversePrimary" to scheme.inversePrimary,
        "primaryFixed" to scheme.primaryFixed,
        "primaryFixedDim" to scheme.primaryFixedDim,
        "onPrimaryFixed" to scheme.onPrimaryFixed,
        "onPrimaryFixedVariant" to scheme.onPrimaryFixedVariant,
        "secondary" to scheme.secondary,
        "onSecondary" to scheme.onSecondary,
        "secondaryContainer" to scheme.secondaryContainer,
        "onSecondaryContainer" to scheme.onSecondaryContainer,
        "secondaryFixed" to scheme.secondaryFixed,
        "secondaryFixedDim" to scheme.secondaryFixedDim,
        "onSecondaryFixed" to scheme.onSecondaryFixed,
        "onSecondaryFixedVariant" to scheme.onSecondaryFixedVariant,
        "tertiary" to scheme.tertiary,
        "onTertiary" to scheme.onTertiary,
        "tertiaryContainer" to scheme.tertiaryContainer,
        "onTertiaryContainer" to scheme.onTertiaryContainer,
        "tertiaryFixed" to scheme.tertiaryFixed,
        "tertiaryFixedDim" to scheme.tertiaryFixedDim,
        "onTertiaryFixed" to scheme.onTertiaryFixed,
        "onTertiaryFixedVariant" to scheme.onTertiaryFixedVariant,
        "error" to scheme.error,
        "onError" to scheme.onError,
        "errorContainer" to scheme.errorContainer,
        "onErrorContainer" to scheme.onErrorContainer,
        "surface" to scheme.surface,
        "onSurface" to scheme.onSurface,
        "surfaceVariant" to scheme.surfaceVariant,
        "onSurfaceVariant" to scheme.onSurfaceVariant,
        "surfaceTint" to scheme.surfaceTint,
        "surfaceBright" to scheme.surfaceBright,
        "surfaceDim" to scheme.surfaceDim,
        "surfaceContainer" to scheme.surfaceContainer,
        "surfaceContainerHigh" to scheme.surfaceContainerHigh,
        "surfaceContainerHighest" to scheme.surfaceContainerHighest,
        "surfaceContainerLow" to scheme.surfaceContainerLow,
        "surfaceContainerLowest" to scheme.surfaceContainerLowest,
        "background" to scheme.background,
        "onBackground" to scheme.onBackground,
        "inverseSurface" to scheme.inverseSurface,
        "inverseOnSurface" to scheme.inverseOnSurface,
        "outline" to scheme.outline,
        "outlineVariant" to scheme.outlineVariant,
        "scrim" to scheme.scrim,
    )
}
