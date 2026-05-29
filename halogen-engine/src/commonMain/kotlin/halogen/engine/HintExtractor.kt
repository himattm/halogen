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

    /**
     * Extracts a hint from the key.
     * Note: Replaced multiple Regexes with manual string iteration and StringBuilder
     * to avoid regex compilation and matching overhead on hot paths.
     */
    fun extract(key: String): String? {
        if (key.isBlank()) return null
        var trimmed = key.trim()

        // Strip common prefixes
        if (trimmed.startsWith("/r/")) trimmed = trimmed.substring(3)
        else if (trimmed.startsWith("/category/")) trimmed = trimmed.substring(10)
        else if (trimmed.startsWith("/topic/")) trimmed = trimmed.substring(7)
        else if (trimmed.startsWith("/")) trimmed = trimmed.substring(1)
        else if (trimmed.startsWith("#")) trimmed = trimmed.substring(1)

        // Remove leading/trailing slashes
        trimmed = trimmed.trim('/')

        // Take the last meaningful segment if it looks like a path
        val lastSlash = trimmed.lastIndexOf('/')
        if (lastSlash != -1) {
            trimmed = trimmed.substring(lastSlash + 1)
        }

        if (trimmed.isEmpty()) return null

        val sb = StringBuilder(trimmed.length * 2)
        var lastWasSpace = true // Start as true to trim leading spaces
        var isAllHex = true
        var isAllNumeric = true
        var lengthWithoutSpaces = 0

        for (i in trimmed.indices) {
            val c = trimmed[i]

            if (c == '_' || c == '-' || c.isWhitespace()) {
                if (!lastWasSpace) {
                    sb.append(' ')
                    lastWasSpace = true
                }
                continue
            }

            if (i > 0 && c.isUpperCase() && trimmed[i-1].isLowerCase()) {
                if (!lastWasSpace) {
                    sb.append(' ')
                }
            }

            val lowerC = c.lowercaseChar()
            sb.append(lowerC)
            lastWasSpace = false
            lengthWithoutSpaces++

            if (lowerC !in '0'..'9') isAllNumeric = false
            if (lowerC !in '0'..'9' && lowerC !in 'a'..'f') isAllHex = false
        }

        // Trim trailing space if any
        var result = sb.toString()
        if (result.endsWith(" ")) {
            result = result.substring(0, result.length - 1)
        }

        if (result.isEmpty()) return null

        // Reject things that look like IDs
        if (isAllNumeric) return null
        if (isAllHex && lengthWithoutSpaces >= 8) return null

        return result
    }
}
