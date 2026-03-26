package halogen.compose

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import halogen.HalogenExtensions

/**
 * Provides [HalogenExtensions] custom tokens through the composition.
 */
public val LocalHalogenExtensions: ProvidableCompositionLocal<HalogenExtensions> =
    staticCompositionLocalOf {
        HalogenExtensions.empty()
    }
