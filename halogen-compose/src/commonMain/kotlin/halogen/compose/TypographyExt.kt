package halogen.compose

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import halogen.HalogenTypography

/**
 * Converts a [HalogenTypography] to a Compose Material 3 [Typography].
 */
public fun HalogenTypography.toMaterial3(): Typography {
    val headingFontWeight = FontWeight(headingWeight.coerceIn(100, 900))
    val bodyFontWeight = FontWeight(bodyWeight.coerceIn(100, 900))
    val letterSpacing = if (tightLetterSpacing) (-0.5).sp else 0.sp

    // Map fontMood to font family
    val displayFamily = when (fontMood) {
        "classic" -> FontFamily.Serif
        "mono" -> FontFamily.Monospace
        else -> FontFamily.SansSerif // modern, playful, minimal all use SansSerif as base
    }

    return Typography(
        displayLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = headingFontWeight,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = letterSpacing,
        ),
        displayMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = headingFontWeight,
            fontSize = 45.sp,
            lineHeight = 52.sp,
            letterSpacing = letterSpacing,
        ),
        displaySmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = headingFontWeight,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = letterSpacing,
        ),
        headlineLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = headingFontWeight,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = letterSpacing,
        ),
        headlineMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = headingFontWeight,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = letterSpacing,
        ),
        headlineSmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = headingFontWeight,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = letterSpacing,
        ),
        titleLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = headingFontWeight,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = letterSpacing,
        ),
        titleMedium = TextStyle(
            fontWeight = bodyFontWeight,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp,
        ),
        titleSmall = TextStyle(
            fontWeight = bodyFontWeight,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
        bodyLarge = TextStyle(
            fontWeight = bodyFontWeight,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp,
        ),
        bodyMedium = TextStyle(
            fontWeight = bodyFontWeight,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp,
        ),
        bodySmall = TextStyle(
            fontWeight = bodyFontWeight,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp,
        ),
        labelLarge = TextStyle(
            fontWeight = bodyFontWeight,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
        labelMedium = TextStyle(
            fontWeight = bodyFontWeight,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
        ),
        labelSmall = TextStyle(
            fontWeight = bodyFontWeight,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
        ),
    )
}
