package halogen

/**
 * Builds the full LLM prompt from a user hint and optional custom extensions.
 */
public object PromptBuilder {

    private const val SYSTEM_PROMPT: String = """You are a Material Design 3 theme designer. Given a user's description, output a JSON color theme. Use ONLY this format:

{
  "pri": "#RRGGBB",
  "sec": "#RRGGBB",
  "ter": "#RRGGBB",
  "neuL": "#RRGGBB",
  "neuD": "#RRGGBB",
  "err": "#RRGGBB",
  "font": "modern",
  "hw": 700,
  "bw": 400,
  "ls": false,
  "cs": "rounded",
  "cx": 1.0
}

Rules:
- Output ONLY the JSON object, no other text
- All colors must be valid 6-digit hex with # prefix
- CRITICAL: pri, sec, and ter must be COLORFUL — never gray, white, or near-white. They must have visible hue and saturation. For "sun" pick yellows/ambers like #C49A2A, for "ocean" pick blues like #356A8A. NEVER #F5F5F5 or similar grays for pri/sec/ter.
- pri (primary): A medium-toned color with clear hue. Think of the color you'd paint a button — clearly blue, green, amber, etc. Good range: #3A-#9A for each channel. Example: #7B6B3A (amber), #3A6B5A (teal)
- sec (secondary): Same hue family as primary but more muted/shifted. Must still have visible color.
- ter (tertiary): A complementary or analogous accent. Must have visible color, not gray.
- neuL: A very light tinted neutral (e.g. #F5F0EB for warm, #F0F4F8 for cool). Can be very light but NOT pure gray like #F5F5F5.
- neuD: A very dark tinted neutral (e.g. #1A1512 for warm, #0F1419 for cool). NOT pure gray.
- err: A muted red like #BA1A1A. NOT bright #FF0000.
- font: One of "modern", "classic", "playful", "minimal", "mono"
- cs (corner style): One of "sharp", "rounded", "pill", "soft"
- Think about what COLORS represent the user's description. "Sun" = warm yellows/ambers. "Ocean" = blues/teals. "Forest" = greens. Always pick recognizable colors."""

    private val FEW_SHOT_EXAMPLES: List<Pair<String, String>> = listOf(
        "ocean vibes, calming" to
            """{"pri":"#356A8A","sec":"#5C8A9E","ter":"#7AACB5","neuL":"#F0F5F7","neuD":"#0E1B26","err":"#BA1A1A","font":"modern","hw":500,"bw":400,"ls":false,"cs":"soft","cx":1.2}""",
        "neon cyberpunk" to
            """{"pri":"#9A6ACD","sec":"#4A8A8A","ter":"#B06B7D","neuL":"#F3F0F6","neuD":"#151018","err":"#93000A","font":"mono","hw":700,"bw":400,"ls":true,"cs":"sharp","cx":0.5}""",
        "warm and cozy, rounded" to
            """{"pri":"#8B5E3C","sec":"#A68B6B","ter":"#6B4E3D","neuL":"#F8F3ED","neuD":"#1C1410","err":"#BA1A1A","font":"classic","hw":600,"bw":400,"ls":false,"cs":"pill","cx":1.5}""",
    )

    /**
     * Build the full prompt for the LLM.
     *
     * @param userHint The user's natural-language theme description.
     * @param extensions Optional custom extension tokens to include in the prompt.
     * @param config Color science configuration controlling style guidance.
     * @param additionalInstructions Extra rules appended after config guidance (e.g., brand constraints).
     * @param customExamples Additional few-shot examples appended after the built-in ones.
     * @return The complete prompt string ready to send to an LLM provider.
     */
    public fun build(
        userHint: String,
        extensions: List<HalogenExtension> = emptyList(),
        config: HalogenConfig = HalogenConfig.Default,
        additionalInstructions: String = "",
        customExamples: List<Pair<String, String>> = emptyList(),
    ): String {
        return buildString {
            append(SYSTEM_PROMPT)
            append("\n\n")

            // Style guidance from config preset
            if (config.promptGuidance.isNotBlank()) {
                append("Style guidance: ")
                append(config.promptGuidance)
                append("\n\n")
            }

            // Additional instructions from the caller
            if (additionalInstructions.isNotBlank()) {
                append("Additional rules:\n")
                append(additionalInstructions)
                append("\n\n")
            }

            for ((input, output) in FEW_SHOT_EXAMPLES) {
                append("User: ")
                append(input)
                append('\n')
                append(output)
                append("\n\n")
            }

            for ((input, output) in customExamples) {
                append("User: ")
                append(input)
                append('\n')
                append(output)
                append("\n\n")
            }

            if (extensions.isNotEmpty()) {
                append("Also include these custom color tokens in an \"ext\" object:\n")
                for (ext in extensions) {
                    append("- \"")
                    append(ext.key)
                    append("\": ")
                    append(ext.description)
                    append('\n')
                }
                append('\n')
            }

            append("User: ")
            // Prepend style name so Nano sees e.g. "vibrant sun" instead of just "sun"
            if (config.styleName.isNotBlank()) {
                append(config.styleName)
                append(' ')
            }
            append(userHint)
            append('\n')
        }
    }

    /**
     * Estimate the token count of a prompt string.
     * Uses a ~4 characters per token heuristic suitable for English text.
     */
    public fun estimateTokenCount(text: String): Int {
        return (text.length + 3) / 4 // ceiling division
    }

    /**
     * Returns a human-readable token estimate breakdown for the prompt components.
     *
     * @param extensions Custom extension tokens.
     * @param config Color science configuration.
     * @param additionalInstructions Extra rules appended after config guidance.
     * @param customExamples Additional few-shot examples.
     * @return A multi-line string with per-component token estimates.
     */
    public fun tokenBreakdown(
        extensions: List<HalogenExtension> = emptyList(),
        config: HalogenConfig = HalogenConfig.Default,
        additionalInstructions: String = "",
        customExamples: List<Pair<String, String>> = emptyList(),
    ): String {
        val systemTokens = estimateTokenCount(SYSTEM_PROMPT)
        val guidanceTokens = if (config.promptGuidance.isNotBlank()) estimateTokenCount(config.promptGuidance) + 5 else 0
        val instructionTokens = if (additionalInstructions.isNotBlank()) estimateTokenCount(additionalInstructions) + 5 else 0
        val builtInExampleTokens = FEW_SHOT_EXAMPLES.sumOf { (input, output) -> estimateTokenCount("User: $input\n$output\n\n") }
        val customExampleTokens = customExamples.sumOf { (input, output) -> estimateTokenCount("User: $input\n$output\n\n") }
        val extensionTokens = extensions.sumOf { estimateTokenCount("- \"${it.key}\": ${it.description}\n") } + if (extensions.isNotEmpty()) 15 else 0

        return buildString {
            appendLine("  System prompt:     ~$systemTokens tokens")
            if (guidanceTokens > 0) appendLine("  Config guidance:   ~$guidanceTokens tokens")
            if (instructionTokens > 0) appendLine("  Instructions:      ~$instructionTokens tokens")
            appendLine("  Built-in examples: ~$builtInExampleTokens tokens")
            if (customExampleTokens > 0) appendLine("  Custom examples:   ~$customExampleTokens tokens")
            if (extensionTokens > 0) appendLine("  Extensions (${extensions.size}):   ~$extensionTokens tokens")
        }
    }
}
