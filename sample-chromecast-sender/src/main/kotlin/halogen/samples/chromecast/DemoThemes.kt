package halogen.samples.chromecast

import halogen.HalogenThemeSpec

/** Hand-picked demo palettes so the sample app runs without an LLM call. */
internal object DemoThemes {
    val ALL: List<Named> = listOf(
        Named(
            "Coffee Shop",
            HalogenThemeSpec(
                primary = "#6F4E37", secondary = "#C08457", tertiary = "#A47551",
                neutralLight = "#F5EFE6", neutralDark = "#2B1D14", error = "#B3261E",
                fontMood = "classic", headingWeight = 600, bodyWeight = 400,
                tightLetterSpacing = false, cornerStyle = "rounded", cornerScale = 1.0f,
            ),
        ),
        Named(
            "Neon Arcade",
            HalogenThemeSpec(
                primary = "#FF1F8F", secondary = "#00E5FF", tertiary = "#FFEA00",
                neutralLight = "#0E0B14", neutralDark = "#0E0B14", error = "#FF5252",
                fontMood = "playful", headingWeight = 800, bodyWeight = 500,
                tightLetterSpacing = true, cornerStyle = "pill", cornerScale = 1.5f,
            ),
        ),
        Named(
            "Forest Morning",
            HalogenThemeSpec(
                primary = "#2E7D32", secondary = "#81C784", tertiary = "#A5D6A7",
                neutralLight = "#F3F7F2", neutralDark = "#1B2720", error = "#C62828",
                fontMood = "minimal", headingWeight = 500, bodyWeight = 400,
                tightLetterSpacing = false, cornerStyle = "soft", cornerScale = 1.0f,
            ),
        ),
        Named(
            "Sunset",
            HalogenThemeSpec(
                primary = "#E94F37", secondary = "#F6AE2D", tertiary = "#F26419",
                neutralLight = "#FFF4EE", neutralDark = "#2B150C", error = "#B00020",
                fontMood = "modern", headingWeight = 700, bodyWeight = 400,
                tightLetterSpacing = true, cornerStyle = "rounded", cornerScale = 1.2f,
            ),
        ),
        Named(
            "Monochrome",
            HalogenThemeSpec(
                primary = "#111111", secondary = "#555555", tertiary = "#AAAAAA",
                neutralLight = "#FAFAFA", neutralDark = "#0A0A0A", error = "#D32F2F",
                fontMood = "monospace", headingWeight = 600, bodyWeight = 400,
                tightLetterSpacing = false, cornerStyle = "sharp", cornerScale = 0.5f,
            ),
        ),
    )

    data class Named(val name: String, val spec: HalogenThemeSpec)
}
