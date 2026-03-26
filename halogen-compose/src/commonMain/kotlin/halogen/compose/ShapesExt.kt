package halogen.compose

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import halogen.HalogenShapes

/**
 * Converts [HalogenShapes] to Compose Material 3 [Shapes].
 */
public fun HalogenShapes.toMaterial3(): Shapes {
    return Shapes(
        extraSmall = RoundedCornerShape(extraSmall.dp),
        small = RoundedCornerShape(small.dp),
        medium = RoundedCornerShape(medium.dp),
        large = RoundedCornerShape(large.dp),
        extraLarge = RoundedCornerShape(extraLarge.dp),
    )
}
