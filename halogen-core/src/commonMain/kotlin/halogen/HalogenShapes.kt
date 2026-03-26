package halogen

/**
 * Shape configuration with five corner-radius sizes matching Material 3.
 * All values are in dp.
 */
public data class HalogenShapes(
    val extraSmall: Float,
    val small: Float,
    val medium: Float,
    val large: Float,
    val extraLarge: Float,
) {
    public companion object {
        /**
         * Derive shapes from a corner style name and scale factor.
         *
         * @param cornerStyle One of "sharp", "rounded", "pill", "soft".
         * @param cornerScale Multiplier clamped to 0.0-2.0.
         */
        public fun fromSpec(cornerStyle: String, cornerScale: Float): HalogenShapes {
            val base = when (cornerStyle) {
                "sharp" -> floatArrayOf(0f, 2f, 4f, 8f, 12f)
                "rounded" -> floatArrayOf(4f, 8f, 16f, 24f, 32f)
                "pill" -> floatArrayOf(8f, 16f, 24f, 48f, 64f)
                "soft" -> floatArrayOf(6f, 12f, 20f, 28f, 36f)
                else -> floatArrayOf(4f, 8f, 16f, 24f, 32f)
            }
            val scale = cornerScale.coerceIn(0f, 2f)
            return HalogenShapes(
                extraSmall = base[0] * scale,
                small = base[1] * scale,
                medium = base[2] * scale,
                large = base[3] * scale,
                extraLarge = base[4] * scale,
            )
        }
    }
}
