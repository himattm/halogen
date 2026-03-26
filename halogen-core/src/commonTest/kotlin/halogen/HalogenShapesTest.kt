package halogen

import kotlin.test.Test
import kotlin.test.assertEquals

class HalogenShapesTest {

    @Test
    fun roundedStyle_producesM3Defaults() {
        val shapes = HalogenShapes.fromSpec("rounded", 1.0f)
        assertEquals(4f, shapes.extraSmall)
        assertEquals(8f, shapes.small)
        assertEquals(16f, shapes.medium)
        assertEquals(24f, shapes.large)
        assertEquals(32f, shapes.extraLarge)
    }

    @Test
    fun sharpStyle_producesNearZeroRadii() {
        val shapes = HalogenShapes.fromSpec("sharp", 1.0f)
        assertEquals(0f, shapes.extraSmall)
        assertEquals(2f, shapes.small)
        assertEquals(4f, shapes.medium)
        assertEquals(8f, shapes.large)
        assertEquals(12f, shapes.extraLarge)
    }

    @Test
    fun pillStyle_producesLargeRadii() {
        val shapes = HalogenShapes.fromSpec("pill", 1.0f)
        assertEquals(8f, shapes.extraSmall)
        assertEquals(16f, shapes.small)
        assertEquals(24f, shapes.medium)
        assertEquals(48f, shapes.large)
        assertEquals(64f, shapes.extraLarge)
    }

    @Test
    fun softStyle_producesIntermediateRadii() {
        val shapes = HalogenShapes.fromSpec("soft", 1.0f)
        assertEquals(6f, shapes.extraSmall)
        assertEquals(12f, shapes.small)
        assertEquals(20f, shapes.medium)
        assertEquals(28f, shapes.large)
        assertEquals(36f, shapes.extraLarge)
    }

    @Test
    fun cornerScale0_producesAllZeros() {
        val shapes = HalogenShapes.fromSpec("rounded", 0.0f)
        assertEquals(0f, shapes.extraSmall)
        assertEquals(0f, shapes.small)
        assertEquals(0f, shapes.medium)
        assertEquals(0f, shapes.large)
        assertEquals(0f, shapes.extraLarge)
    }

    @Test
    fun cornerScale2_doublesValues() {
        val shapes = HalogenShapes.fromSpec("rounded", 2.0f)
        assertEquals(8f, shapes.extraSmall)
        assertEquals(16f, shapes.small)
        assertEquals(32f, shapes.medium)
        assertEquals(48f, shapes.large)
        assertEquals(64f, shapes.extraLarge)
    }

    @Test
    fun cornerScale_clampedTo2() {
        val shapes = HalogenShapes.fromSpec("rounded", 5.0f)
        // Should be clamped to scale=2.0
        assertEquals(8f, shapes.extraSmall)
        assertEquals(16f, shapes.small)
        assertEquals(32f, shapes.medium)
        assertEquals(48f, shapes.large)
        assertEquals(64f, shapes.extraLarge)
    }

    @Test
    fun cornerScale_clampedTo0() {
        val shapes = HalogenShapes.fromSpec("rounded", -3.0f)
        // Should be clamped to scale=0.0
        assertEquals(0f, shapes.extraSmall)
        assertEquals(0f, shapes.small)
        assertEquals(0f, shapes.medium)
        assertEquals(0f, shapes.large)
        assertEquals(0f, shapes.extraLarge)
    }

    @Test
    fun unknownStyle_defaultsToRounded() {
        val shapes = HalogenShapes.fromSpec("totally_unknown", 1.0f)
        val rounded = HalogenShapes.fromSpec("rounded", 1.0f)
        assertEquals(rounded, shapes, "Unknown style should fall back to rounded defaults")
    }

    @Test
    fun cornerScale_halfMultiplier() {
        val shapes = HalogenShapes.fromSpec("rounded", 0.5f)
        assertEquals(2f, shapes.extraSmall)
        assertEquals(4f, shapes.small)
        assertEquals(8f, shapes.medium)
        assertEquals(12f, shapes.large)
        assertEquals(16f, shapes.extraLarge)
    }
}
