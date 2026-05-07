package halogen.image

/**
 * A color extracted from an image, with its population weight and HCT coordinates.
 *
 * @property argb The ARGB integer representation of this color.
 * @property population The relative population of this color in the image, from 0.0 to 1.0.
 * @property hue The HCT hue in degrees, from 0 to 360.
 * @property chroma The HCT chroma (colorfulness). Unbounded, but typically 0-120.
 * @property tone The HCT tone (lightness), from 0 to 100.
 */
public data class QuantizedColor(
    val argb: Int,
    val population: Double,
    val hue: Double,
    val chroma: Double,
    val tone: Double,
)
