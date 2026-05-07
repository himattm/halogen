package halogen.image

import kotlin.math.abs
import kotlin.math.min

/** Shortest angular distance between two hues, wrapping at 360 degrees. */
internal fun hueDist(h1: Double, h2: Double): Double =
    min(abs(h1 - h2), 360.0 - abs(h1 - h2))
