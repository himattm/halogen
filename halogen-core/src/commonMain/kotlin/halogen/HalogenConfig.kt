package halogen

/**
 * Configuration for how Halogen expands LLM seed colors into Material 3 palettes.
 *
 * These parameters control the chroma (saturation) levels applied to each palette role
 * during theme expansion. The LLM provides hue direction; these caps ensure the resulting
 * theme follows Material Design principles for readability, harmony, and accessibility.
 *
 * ## When to customize
 *
 * The defaults follow M3's SchemeTonalSpot and work well for most apps. Customize when:
 * - Your brand requires more vibrant colors: increase [primaryMaxChroma]
 * - You want a more monochromatic look: decrease secondary/tertiary chroma
 * - You want more colorful surfaces: increase [neutralChroma]
 * - You want outlines to feel more connected to the theme: increase [neutralVariantChroma]
 *
 * ## Chroma reference
 *
 * Chroma in HCT is unbounded, but practical ranges for UI:
 * - **0**: Pure gray (no color)
 * - **4-8**: Barely tinted gray (neutral surfaces)
 * - **16**: Subtly colored (muted secondary)
 * - **24-36**: Clearly colored but not loud (accents)
 * - **48**: Vibrant, button-appropriate color
 * - **84+**: Very saturated (error red)
 *
 * @property primaryMaxChroma Maximum chroma for the primary palette. Controls how vibrant
 *   buttons, active states, and primary surfaces appear. Default: 48.
 * @property secondaryMaxChroma Maximum chroma for the secondary palette. Should be
 *   significantly lower than primary to create visual hierarchy. Default: 16.
 * @property tertiaryMaxChroma Maximum chroma for the tertiary (accent) palette.
 *   Sits between primary and secondary intensity. Default: 24.
 * @property neutralChroma Fixed chroma for the neutral palette used by surfaces,
 *   backgrounds, and containers. Very low for near-gray surfaces with a subtle
 *   theme tint. Default: 6.
 * @property neutralVariantChroma Fixed chroma for the neutral-variant palette used by
 *   outlines, surface variants, and dividers. Slightly higher than [neutralChroma] so
 *   these elements feel connected to the theme color. Default: 8.
 * @property errorHue Fixed hue for the error palette. M3 uses 25 (red family) regardless
 *   of theme. Set to `null` to use the LLM's error seed hue instead. Default: 25.
 * @property errorChroma Chroma for the error palette. High chroma ensures error states
 *   are clearly distinguishable. Default: 84.
 * @property useFixedError When true, the error palette uses [errorHue] and [errorChroma]
 *   regardless of what the LLM returns. When false, the LLM's error seed hue is used
 *   with chroma capped at [errorChroma]. Default: true.
 */
public data class HalogenConfig(
    val primaryMaxChroma: Double = DEFAULT_PRIMARY_MAX_CHROMA,
    val secondaryMaxChroma: Double = DEFAULT_SECONDARY_MAX_CHROMA,
    val tertiaryMaxChroma: Double = DEFAULT_TERTIARY_MAX_CHROMA,
    val neutralChroma: Double = DEFAULT_NEUTRAL_CHROMA,
    val neutralVariantChroma: Double = DEFAULT_NEUTRAL_VARIANT_CHROMA,
    val errorHue: Double? = DEFAULT_ERROR_HUE,
    val errorChroma: Double = DEFAULT_ERROR_CHROMA,
    val useFixedError: Boolean = true,
    /**
     * Additional prompt guidance appended to the system prompt to steer the LLM
     * toward style-appropriate colors. Empty string means no extra guidance.
     * Each preset includes guidance tuned for its style.
     */
    val promptGuidance: String = "",
    /**
     * Display name for this config preset. Used in the LLM prompt to hint
     * the desired style (e.g., "vibrant sun" instead of just "sun").
     * Empty string means no style prefix is added to the prompt.
     */
    val styleName: String = "",
    /**
     * Minimum chroma for the primary palette. If the LLM returns a near-gray
     * primary seed (common when it picks white/light colors), the chroma is
     * boosted to at least this value so the theme has visible color identity.
     * Set to 0 to disable. Default: 32.
     */
    val primaryMinChroma: Double = DEFAULT_PRIMARY_MIN_CHROMA,
    /**
     * Whether to apply LLM-generated colors. When `false`, the default M3
     * color scheme is used regardless of the spec. Default: `true`.
     */
    val useGeneratedColors: Boolean = true,
    /**
     * Whether to apply LLM-generated shapes (corner radii). When `false`, the
     * default M3 shapes are used regardless of the spec. Default: `false`.
     */
    val useGeneratedShapes: Boolean = false,
    /**
     * Whether to apply LLM-generated typography (font family, weight, spacing).
     * When `false`, the default M3 typography is used regardless of the spec.
     * Default: `false`.
     */
    val useGeneratedTypography: Boolean = false,
) {
    public companion object {
        /** M3 SchemeTonalSpot defaults — the recommended baseline. */
        public const val DEFAULT_PRIMARY_MIN_CHROMA: Double = 32.0
        public const val DEFAULT_PRIMARY_MAX_CHROMA: Double = 48.0
        public const val DEFAULT_SECONDARY_MAX_CHROMA: Double = 16.0
        public const val DEFAULT_TERTIARY_MAX_CHROMA: Double = 24.0
        public const val DEFAULT_NEUTRAL_CHROMA: Double = 6.0
        public const val DEFAULT_NEUTRAL_VARIANT_CHROMA: Double = 8.0
        public const val DEFAULT_ERROR_HUE: Double = 25.0
        public const val DEFAULT_ERROR_CHROMA: Double = 84.0

        /**
         * Default configuration following M3's SchemeTonalSpot. Recommended for most apps.
         */
        // ── Presets ──
        // Each preset sets BOTH min and max chroma so the expansion actively
        // pushes colors toward the desired intensity, not just clips them.

        public val Default: HalogenConfig = HalogenConfig(
            styleName = "",
            primaryMinChroma = 32.0,
            primaryMaxChroma = 48.0,
            secondaryMaxChroma = 16.0,
            tertiaryMaxChroma = 24.0,
            promptGuidance = "Pick colors with medium saturation that feel natural and balanced. " +
                "Primary should be clearly recognizable — avoid grays or very dark colors for primary.",
        )

        public val Vibrant: HalogenConfig = HalogenConfig(
            styleName = "vibrant",
            primaryMinChroma = 56.0,
            primaryMaxChroma = 64.0,
            secondaryMaxChroma = 24.0,
            tertiaryMaxChroma = 36.0,
            neutralChroma = 10.0,
            neutralVariantChroma = 12.0,
            promptGuidance = "Pick bold, saturated, vivid colors. Primary should be eye-catching and rich. " +
                "Secondary should complement it with a different but harmonious hue. Be expressive.",
        )

        public val Muted: HalogenConfig = HalogenConfig(
            styleName = "muted",
            primaryMinChroma = 16.0,
            primaryMaxChroma = 36.0,
            secondaryMaxChroma = 12.0,
            tertiaryMaxChroma = 16.0,
            neutralChroma = 4.0,
            neutralVariantChroma = 6.0,
            promptGuidance = "Pick desaturated, understated colors. Think corporate, calm, professional. " +
                "Colors should whisper, not shout. Avoid anything bright or playful.",
        )

        public val Monochrome: HalogenConfig = HalogenConfig(
            styleName = "monochrome",
            primaryMinChroma = 16.0,
            primaryMaxChroma = 36.0,
            secondaryMaxChroma = 8.0,
            tertiaryMaxChroma = 12.0,
            neutralChroma = 2.0,
            neutralVariantChroma = 4.0,
            promptGuidance = "Pick colors that are all variations of the SAME hue family. " +
                "Secondary and tertiary should be the same hue as primary but lighter or darker. " +
                "The entire theme should feel like one color at different intensities.",
        )

        public val Punchy: HalogenConfig = HalogenConfig(
            styleName = "punchy",
            primaryMinChroma = 64.0,
            primaryMaxChroma = 72.0,
            secondaryMaxChroma = 32.0,
            tertiaryMaxChroma = 48.0,
            neutralChroma = 8.0,
            neutralVariantChroma = 12.0,
            promptGuidance = "Pick BOLD, high-energy colors with strong contrast between them. " +
                "Think gaming, sports, action. Primary should pop. " +
                "Tertiary should be a contrasting accent that creates visual excitement.",
        )

        public val Pastel: HalogenConfig = HalogenConfig(
            styleName = "pastel",
            primaryMinChroma = 12.0,
            primaryMaxChroma = 24.0,
            secondaryMaxChroma = 16.0,
            tertiaryMaxChroma = 20.0,
            neutralChroma = 4.0,
            neutralVariantChroma = 6.0,
            promptGuidance = "Pick soft, pastel-like colors. Think light pink, baby blue, mint, lavender. " +
                "Everything should feel gentle and airy. Avoid dark or intense colors. " +
                "Choose light, warm hues with low saturation.",
        )

        public val Editorial: HalogenConfig = HalogenConfig(
            styleName = "editorial",
            primaryMinChroma = 36.0,
            primaryMaxChroma = 48.0,
            secondaryMaxChroma = 8.0,
            tertiaryMaxChroma = 12.0,
            neutralChroma = 3.0,
            neutralVariantChroma = 5.0,
            promptGuidance = "Pick a single strong primary color and keep everything else very neutral. " +
                "Secondary and tertiary should be nearly gray. " +
                "The design should feel like a newspaper or magazine — content is king.",
        )

        public val Expressive: HalogenConfig = HalogenConfig(
            styleName = "expressive",
            primaryMinChroma = 48.0,
            primaryMaxChroma = 56.0,
            secondaryMaxChroma = 24.0,
            tertiaryMaxChroma = 36.0,
            neutralChroma = 12.0,
            neutralVariantChroma = 16.0,
            promptGuidance = "Pick colorful, fun, expressive colors. All three colors should be distinct and lively. " +
                "Even neutrals should feel tinted. Think social media, creative tools, entertainment. " +
                "The whole UI should feel alive with color.",
        )

        /**
         * All available presets, keyed by display name. Useful for building UI pickers.
         */
        public val presets: Map<String, HalogenConfig> = mapOf(
            "Default" to Default,
            "Vibrant" to Vibrant,
            "Muted" to Muted,
            "Monochrome" to Monochrome,
            "Punchy" to Punchy,
            "Pastel" to Pastel,
            "Editorial" to Editorial,
            "Expressive" to Expressive,
        )
    }
}
