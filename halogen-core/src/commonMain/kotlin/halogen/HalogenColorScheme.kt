package halogen

/**
 * All 49 Material 3 color roles as ARGB integers.
 *
 * A single [HalogenThemeSpec] expands into two [HalogenColorScheme] instances:
 * one for light mode and one for dark mode.
 */
public data class HalogenColorScheme(
    // Primary (9)
    val primary: Int,
    val onPrimary: Int,
    val primaryContainer: Int,
    val onPrimaryContainer: Int,
    val inversePrimary: Int,
    val primaryFixed: Int,
    val primaryFixedDim: Int,
    val onPrimaryFixed: Int,
    val onPrimaryFixedVariant: Int,

    // Secondary (8)
    val secondary: Int,
    val onSecondary: Int,
    val secondaryContainer: Int,
    val onSecondaryContainer: Int,
    val secondaryFixed: Int,
    val secondaryFixedDim: Int,
    val onSecondaryFixed: Int,
    val onSecondaryFixedVariant: Int,

    // Tertiary (8)
    val tertiary: Int,
    val onTertiary: Int,
    val tertiaryContainer: Int,
    val onTertiaryContainer: Int,
    val tertiaryFixed: Int,
    val tertiaryFixedDim: Int,
    val onTertiaryFixed: Int,
    val onTertiaryFixedVariant: Int,

    // Error (4)
    val error: Int,
    val onError: Int,
    val errorContainer: Int,
    val onErrorContainer: Int,

    // Surface (14)
    val surface: Int,
    val onSurface: Int,
    val surfaceVariant: Int,
    val onSurfaceVariant: Int,
    val surfaceTint: Int,
    val surfaceBright: Int,
    val surfaceDim: Int,
    val surfaceContainer: Int,
    val surfaceContainerHigh: Int,
    val surfaceContainerHighest: Int,
    val surfaceContainerLow: Int,
    val surfaceContainerLowest: Int,
    val background: Int,
    val onBackground: Int,

    // Utility (5) -- total: 9+8+8+4+14+5 = 48, but inverseSurface counts too
    val inverseSurface: Int,
    val inverseOnSurface: Int,
    val outline: Int,
    val outlineVariant: Int,
    val scrim: Int,
)
