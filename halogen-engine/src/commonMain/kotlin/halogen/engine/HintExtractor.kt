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

    fun extract(key: String): String? {
        if (key.isBlank()) return null

        var cleaned = key.trim()

        // Strip common prefixes
        when {
            cleaned.startsWith("/r/") -> cleaned = cleaned.substring(3)
            cleaned.startsWith("/category/") -> cleaned = cleaned.substring(10)
            cleaned.startsWith("/topic/") -> cleaned = cleaned.substring(7)
            cleaned.startsWith("/") -> cleaned = cleaned.substring(1)
            cleaned.startsWith("#") -> cleaned = cleaned.substring(1)
        }

        // Remove leading/trailing slashes
        cleaned = cleaned.trim('/')

        // Take the last meaningful segment if it looks like a path
        val lastSlash = cleaned.lastIndexOf('/')
        if (lastSlash != -1) {
            cleaned = cleaned.substring(lastSlash + 1)
        }

        if (cleaned.isBlank()) return null

        // Split camelCase, snake_case, kebab-case, normalize whitespace
        val sb = StringBuilder(cleaned.length + 5)
        var lastChar = '\u0000'
        var spacePending = false

        for (i in cleaned.indices) {
            val c = cleaned[i]

            if (c == '_' || c == '-' || c.isWhitespace()) {
                spacePending = true
            } else if (c.isUpperCase() && lastChar.isLowerCase()) {
                if (sb.isNotEmpty()) sb.append(' ')
                sb.append(c.lowercaseChar())
                spacePending = false
            } else {
                if (spacePending && sb.isNotEmpty()) {
                    sb.append(' ')
                }
                sb.append(c.lowercaseChar())
                spacePending = false
            }
            if (c != '_' && c != '-' && !c.isWhitespace()) {
                lastChar = c
            }
        }

        val result = sb.toString()
        if (result.isBlank()) return null

        // Reject things that look like IDs
        var isAllHex = true
        var isAllDigit = true
        var noSpaceLength = 0

        for (i in result.indices) {
            val c = result[i]
            if (c == ' ') continue
            noSpaceLength++
            if (!(c in '0'..'9' || c in 'a'..'f')) {
                isAllHex = false
            }
            if (!(c in '0'..'9')) {
                isAllDigit = false
            }
        }

        if (noSpaceLength >= 8 && isAllHex) return null
        if (noSpaceLength > 0 && isAllDigit) return null

        return result
    }
}
