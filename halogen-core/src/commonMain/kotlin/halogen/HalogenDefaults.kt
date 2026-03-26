package halogen

/**
 * Built-in default [HalogenThemeSpec] values.
 * Uses standard Material 3 / Material You default colors.
 */
public object HalogenDefaults {

    /**
     * Material 3 default light theme spec.
     * Primary: #6750A4, Secondary: #625B71, Tertiary: #7D5260,
     * NeutralLight: #FFFBFE, NeutralDark: #1C1B1F, Error: #B3261E.
     */
    public fun light(): HalogenThemeSpec = HalogenThemeSpec(
        primary = "#6750A4",
        secondary = "#625B71",
        tertiary = "#7D5260",
        neutralLight = "#FFFBFE",
        neutralDark = "#1C1B1F",
        error = "#B3261E",
        fontMood = "modern",
        headingWeight = 400,
        bodyWeight = 400,
        tightLetterSpacing = false,
        cornerStyle = "rounded",
        cornerScale = 1.0f,
    )

    /**
     * Alias for [light], representing the default Android Material You theme.
     */
    public fun materialYou(): HalogenThemeSpec = light()
}
