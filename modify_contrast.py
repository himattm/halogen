with open("halogen-core/src/commonMain/kotlin/halogen/ContrastValidator.kt", "r") as f:
    content = f.read()

# Add comments for the optimization and remove the unused `linearize(Double)` method

new_lut = """    /**
     * Pre-computed Lookup Table (LUT) for sRGB linearization.
     *
     * Optimization: The relative luminance calculation requires converting 8-bit color components (0-255)
     * from sRGB to linear space. Previously, this was done dynamically using floating-point math and expensive
     * `Double.pow(2.4)` operations. Since the input domain is strictly limited to 256 discrete values,
     * we can pre-compute all possible results into this array. This reduces the calculation from complex
     * math to a simple O(1) array lookup, significantly improving performance in this KMP hot path.
     */
    private val LINEARIZE_LUT = DoubleArray(256) { i ->
        val component = i / 255.0
        if (component <= 0.04045) {
            component / 12.92
        } else {
            ((component + 0.055) / 1.055).pow(2.4)
        }
    }

    private fun linearizeInt(component: Int): Double {
        return LINEARIZE_LUT[component and 0xFF]
    }
"""

content = content.replace("    private val LINEARIZE_LUT = DoubleArray(256) { i ->\n        val component = i / 255.0\n        if (component <= 0.04045) {\n            component / 12.92\n        } else {\n            ((component + 0.055) / 1.055).pow(2.4)\n        }\n    }\n\n    private fun linearizeInt(component: Int): Double {\n        return LINEARIZE_LUT[component and 0xFF]\n    }\n\n    private fun linearize(component: Double): Double {\n        return if (component <= 0.04045) {\n            component / 12.92\n        } else {\n            ((component + 0.055) / 1.055).pow(2.4)\n        }\n    }\n", new_lut)


with open("halogen-core/src/commonMain/kotlin/halogen/ContrastValidator.kt", "w") as f:
    f.write(content)
