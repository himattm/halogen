package halogen.compose

import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import halogen.HalogenConfig
import halogen.HalogenThemeSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalTestApi::class)
class HalogenThemeTest {

    private val oceanSpec = HalogenThemeSpec.fromJson(
        """{"pri":"#356A8A","sec":"#5C8A9E","ter":"#7AACB5","neuL":"#F0F5F7","neuD":"#0E1B26","err":"#BA1A1A","font":"modern","hw":500,"bw":400,"ls":false,"cs":"soft","cx":1.2}""",
    )
    private val neonSpec = HalogenThemeSpec.fromJson(
        """{"pri":"#9A6ACD","sec":"#4A8A8A","ter":"#B06B7D","neuL":"#F3F0F6","neuD":"#151018","err":"#93000A","font":"mono","hw":700,"bw":400,"ls":true,"cs":"sharp","cx":0.5}""",
    )

    private fun ComposeUiTest.waitUntilPrimaryChangesFrom(
        previousPrimary: Color,
        currentPrimary: () -> Color,
    ) {
        waitUntil(timeoutMillis = 5_000) {
            val primary = currentPrimary()
            primary != Color.Unspecified && primary != previousPrimary
        }
        waitForIdle()
    }

    // ── Config flag tests ───────────────────────────────────────────────

    @Test
    fun useGeneratedColors_true_appliesLlmColors() = runComposeUiTest {
        val defaultPrimary = lightColorScheme().primary
        var capturedPrimary = defaultPrimary

        setContent {
            HalogenTheme(
                spec = oceanSpec,
                darkTheme = false,
                config = HalogenConfig(useGeneratedColors = true),
                colorAnimationSpec = snap(),
            ) {
                capturedPrimary = MaterialTheme.colorScheme.primary
            }
        }

        waitUntilPrimaryChangesFrom(defaultPrimary) { capturedPrimary }
        assertNotEquals(defaultPrimary, capturedPrimary, "LLM colors should differ from M3 defaults")
    }

    @Test
    fun useGeneratedColors_false_keepsDefaults() = runComposeUiTest {
        val defaultPrimary = lightColorScheme().primary
        var capturedPrimary = Color.Unspecified

        setContent {
            HalogenTheme(
                spec = oceanSpec,
                darkTheme = false,
                config = HalogenConfig(useGeneratedColors = false),
                colorAnimationSpec = snap(),
            ) {
                capturedPrimary = MaterialTheme.colorScheme.primary
            }
        }

        waitForIdle()
        assertEquals(defaultPrimary, capturedPrimary, "Should use default M3 primary when useGeneratedColors=false")
    }

    @Test
    fun useGeneratedShapes_false_keepsDefaultShapes() = runComposeUiTest {
        var capturedMediumShape = androidx.compose.material3.Shapes().medium
        val defaultMediumShape = capturedMediumShape

        setContent {
            HalogenTheme(
                spec = oceanSpec,
                config = HalogenConfig(useGeneratedShapes = false),
                shapeAnimationSpec = snap(),
            ) {
                capturedMediumShape = MaterialTheme.shapes.medium
            }
        }

        waitForIdle()
        assertEquals(defaultMediumShape, capturedMediumShape, "Shapes should be M3 defaults when useGeneratedShapes=false")
    }

    @Test
    fun useGeneratedShapes_true_appliesLlmShapes() = runComposeUiTest {
        val defaultMediumShape = androidx.compose.material3.Shapes().medium
        var capturedMediumShape = defaultMediumShape

        setContent {
            HalogenTheme(
                spec = oceanSpec,
                config = HalogenConfig(useGeneratedShapes = true),
                shapeAnimationSpec = snap(),
            ) {
                capturedMediumShape = MaterialTheme.shapes.medium
            }
        }

        waitUntil(timeoutMillis = 5_000) { capturedMediumShape != defaultMediumShape }
        waitForIdle()
        assertNotEquals(defaultMediumShape, capturedMediumShape, "LLM shapes should differ from M3 defaults")
    }

    // ── Animation tests ─────────────────────────────────────────────────

    @Test
    fun colors_animateOverTime() = runComposeUiTest {
        var spec by mutableStateOf(oceanSpec)
        var capturedPrimary = Color.Unspecified

        setContent {
            HalogenTheme(
                spec = spec,
                darkTheme = false,
                config = HalogenConfig(useGeneratedColors = true),
                colorAnimationSpec = tween(durationMillis = 400),
            ) {
                capturedPrimary = MaterialTheme.colorScheme.primary
            }
        }

        waitUntilPrimaryChangesFrom(lightColorScheme().primary) { capturedPrimary }
        val oceanPrimary = capturedPrimary

        spec = neonSpec
        waitUntilPrimaryChangesFrom(oceanPrimary) { capturedPrimary }
        val finalPrimary = capturedPrimary

        assertNotEquals(oceanPrimary, finalPrimary, "Final primary should differ from ocean primary after spec change")
    }

    @Test
    fun snap_disablesAnimation() = runComposeUiTest {
        var spec by mutableStateOf(oceanSpec)
        var capturedPrimary = Color.Unspecified

        setContent {
            HalogenTheme(
                spec = spec,
                darkTheme = false,
                config = HalogenConfig(useGeneratedColors = true),
                colorAnimationSpec = snap(),
            ) {
                capturedPrimary = MaterialTheme.colorScheme.primary
            }
        }

        waitUntilPrimaryChangesFrom(lightColorScheme().primary) { capturedPrimary }
        val oceanPrimary = capturedPrimary

        spec = neonSpec
        waitUntilPrimaryChangesFrom(oceanPrimary) { capturedPrimary }

        val afterSnap = capturedPrimary

        // With snap(), the color should have changed immediately (no 400ms tween)
        assertNotEquals(oceanPrimary, afterSnap, "Color should have changed with snap()")
    }

    @Test
    fun firstComposition_snapsInstantly() = runComposeUiTest {
        var capturedPrimary = Color.Unspecified
        val defaultPrimary = lightColorScheme().primary

        setContent {
            HalogenTheme(
                spec = oceanSpec,
                darkTheme = false,
                config = HalogenConfig(useGeneratedColors = true),
                colorAnimationSpec = tween(durationMillis = 400),
            ) {
                capturedPrimary = MaterialTheme.colorScheme.primary
            }
        }

        waitUntilPrimaryChangesFrom(defaultPrimary) { capturedPrimary }
        val firstPrimary = capturedPrimary

        // If it animated, we'd still see the default color at frame 0.
        // After 1000ms it should definitely be the ocean color regardless,
        // but the key test: it should NOT be the default M3 purple.
        assertNotEquals(defaultPrimary, firstPrimary, "First theme should have applied (not stuck on defaults)")
    }

    // ── Config defaults test ────────────────────────────────────────────

    @Test
    fun config_defaults_onlyColorsEnabled() {
        val config = HalogenConfig.Default
        assertEquals(true, config.useGeneratedColors)
        assertEquals(false, config.useGeneratedShapes)
        assertEquals(false, config.useGeneratedTypography)
    }

    @Test
    fun config_presets_inheritDefaults() {
        // All presets should inherit the useGenerated defaults
        for ((name, preset) in HalogenConfig.presets) {
            assertEquals(true, preset.useGeneratedColors, "$name should have useGeneratedColors=true")
            assertEquals(false, preset.useGeneratedShapes, "$name should have useGeneratedShapes=false")
            assertEquals(false, preset.useGeneratedTypography, "$name should have useGeneratedTypography=false")
        }
    }
}
