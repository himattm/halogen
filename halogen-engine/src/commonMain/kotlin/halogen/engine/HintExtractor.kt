package halogen.engine

/**
 * Extracts a human-readable hint from a key string (e.g., a URL path or category slug).
 *
 * Rules:
 * - Strip common prefixes: /r/, /category/, /topic/, #
 * - Split camelCase, snake_case, and kebab-case into words
 * - Return null if the result is empty or looks like a random ID
 */
internal object HintExtractor {

    private val PREFIX_PATTERN = Regex("""^(?:/r/|/category/|/topic/|/|#)""")
    private val CAMEL_SPLIT = Regex("""(?<=[a-z])(?=[A-Z])""")
    private val ID_PATTERN = Regex("""^[0-9a-f]{8,}$""", RegexOption.IGNORE_CASE)
    private val NUMERIC_ONLY = Regex("""^\d+$""")

    fun extract(key: String): String? {
        if (key.isBlank()) return null

        // Strip common prefixes
        var cleaned = PREFIX_PATTERN.replace(key.trim(), "")

        // Remove leading/trailing slashes
        cleaned = cleaned.trim('/')

        // Take the last meaningful segment if it looks like a path
        if ('/' in cleaned) {
            cleaned = cleaned.substringAfterLast('/')
        }

        // Split camelCase
        cleaned = CAMEL_SPLIT.replace(cleaned, " ")

        // Split snake_case and kebab-case
        cleaned = cleaned.replace('_', ' ').replace('-', ' ')

        // Normalize whitespace
        cleaned = cleaned.trim().replace(Regex("""\s+"""), " ")

        if (cleaned.isBlank()) return null

        // Reject things that look like IDs
        val noSpaces = cleaned.replace(" ", "")
        if (ID_PATTERN.matches(noSpaces)) return null
        if (NUMERIC_ONLY.matches(noSpaces)) return null

        return cleaned.lowercase()
    }
}
