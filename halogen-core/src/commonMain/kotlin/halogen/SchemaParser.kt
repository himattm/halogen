package halogen

import kotlinx.serialization.json.Json

/**
 * Parses and validates LLM JSON output into [HalogenThemeSpec].
 *
 * Handles common LLM quirks like markdown code fences, and validates
 * all fields are within expected ranges.
 */
public object SchemaParser {

    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val HEX_COLOR_REGEX: Regex = Regex("^#[0-9A-Fa-f]{6}$")

    /**
     * Parse a JSON string (potentially wrapped in markdown code fences) into
     * a validated [HalogenThemeSpec].
     *
     * @return [Result.success] with the parsed and clamped spec, or
     *         [Result.failure] with a descriptive error message.
     */
    public fun parse(json: String): Result<HalogenThemeSpec> {
        return try {
            val cleaned = stripCodeFences(json.trim())
            val spec = this.json.decodeFromString(HalogenThemeSpec.serializer(), cleaned)
            validate(spec)
        } catch (e: Exception) {
            Result.failure(
                IllegalArgumentException("Failed to parse HalogenThemeSpec: ${e.message}", e),
            )
        }
    }

    // ⚡ Bolt: Fast string extraction avoids O(N^2) backtracking from `[\s\S]*?` regex
    private fun stripCodeFences(input: String): String {
        val startIdx = input.indexOf("```")
        if (startIdx == -1) return input

        var payloadStart = startIdx + 3

        // Skip optional language identifier like "json"
        if (input.startsWith("json", payloadStart)) {
            payloadStart += 4
        }

        // Skip optional whitespace/newline right after fence or language identifier
        while (payloadStart < input.length && input[payloadStart].isWhitespace()) {
            payloadStart++
        }

        val endIdx = input.indexOf("```", payloadStart)
        if (endIdx == -1) return input

        return input.substring(payloadStart, endIdx).trim()
    }

    private fun validate(spec: HalogenThemeSpec): Result<HalogenThemeSpec> {
        val colorFields = listOf(
            "primary" to spec.primary,
            "secondary" to spec.secondary,
            "tertiary" to spec.tertiary,
            "neutralLight" to spec.neutralLight,
            "neutralDark" to spec.neutralDark,
            "error" to spec.error,
        )

        for ((name, value) in colorFields) {
            if (!HEX_COLOR_REGEX.matches(value)) {
                return Result.failure(
                    IllegalArgumentException(
                        "Invalid hex color for $name: \"$value\". Expected format: #RRGGBB",
                    ),
                )
            }
        }

        // Clamp values to valid ranges
        val clamped = spec.copy(
            headingWeight = spec.headingWeight.coerceIn(100, 900),
            bodyWeight = spec.bodyWeight.coerceIn(100, 900),
            cornerScale = spec.cornerScale.coerceIn(0.0f, 2.0f),
        )

        return Result.success(clamped)
    }
}
