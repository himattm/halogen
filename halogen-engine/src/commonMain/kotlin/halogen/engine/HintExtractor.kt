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

    private fun isHexId(str: String): Boolean {
        if (str.length < 8) return false
        for (i in 0 until str.length) {
            val c = str[i]
            if (!(c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F')) return false
        }
        return true
    }

    private fun isNumeric(str: String): Boolean {
        if (str.isEmpty()) return false
        for (i in 0 until str.length) {
            val c = str[i]
            if (c !in '0'..'9') return false
        }
        return true
    }

    fun extract(key: String): String? {
        if (key.isBlank()) return null

        // Strip common prefixes
        var cleaned = PREFIX_PATTERN.replace(key.trim(), "")

        // Remove leading/trailing slashes
        cleaned = cleaned.trim('/')

        // Take the last meaningful segment if it looks like a path
        val lastSlash = cleaned.lastIndexOf('/')
        if (lastSlash != -1) {
            cleaned = cleaned.substring(lastSlash + 1)
        }

        // Fast path for splitting camelCase, snake_case, kebab-case and whitespace normalization
        val sb = StringBuilder(cleaned.length * 2)
        var lastWasSpace = true
        var spaceCount = 0

        for (i in 0 until cleaned.length) {
            val c = cleaned[i]
            if (c == '_' || c == '-' || c.isWhitespace()) {
                if (!lastWasSpace) {
                    sb.append(' ')
                    lastWasSpace = true
                    spaceCount++
                }
            } else {
                if (c.isUpperCase() && i > 0 && cleaned[i - 1].isLowerCase()) {
                    if (!lastWasSpace) {
                        sb.append(' ')
                        lastWasSpace = true
                        spaceCount++
                    }
                }
                sb.append(c.lowercaseChar())
                lastWasSpace = false
            }
        }

        cleaned = sb.toString().trim()
        if (cleaned.isBlank()) return null

        // Reject things that look like IDs
        val noSpaces = if (spaceCount > 0) cleaned.replace(" ", "") else cleaned
        if (isHexId(noSpaces)) return null
        if (isNumeric(noSpaces)) return null

        return cleaned
    }
}
