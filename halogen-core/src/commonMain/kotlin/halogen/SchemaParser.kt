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

    private fun isValidHexColor(value: String): Boolean {
        if (value.length != 7) return false
        if (value[0] != '#') return false
        for (i in 1..6) {
            val c = value[i]
            if (!(c in '0'..'9' || c in 'A'..'F' || c in 'a'..'f')) {
                return false
            }
        }
        return true
    }

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

    private fun stripCodeFences(input: String): String {
        val startIndex = input.indexOf("```")
        if (startIndex == -1) return input

        var contentStart = startIndex + 3

        while (contentStart < input.length && input[contentStart].isWhitespace() && input[contentStart] != '\n') {
            contentStart++
        }

        if (input.regionMatches(contentStart, "json", 0, 4, ignoreCase = true)) {
            contentStart += 4
        }

        val endIndex = input.indexOf("```", contentStart)
        if (endIndex == -1) return input

        return input.substring(contentStart, endIndex).trim()
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
            if (!isValidHexColor(value)) {
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
