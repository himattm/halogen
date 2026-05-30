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

        val trimmed = key.trim()
        var startIdx = 0

        // Strip common prefixes
        if (trimmed.startsWith("/r/")) startIdx = 3
        else if (trimmed.startsWith("/category/")) startIdx = 10
        else if (trimmed.startsWith("/topic/")) startIdx = 7
        else if (trimmed.startsWith("/")) startIdx = 1
        else if (trimmed.startsWith("#")) startIdx = 1

        // Trim leading slashes if any
        while (startIdx < trimmed.length && trimmed[startIdx] == '/') {
            startIdx++
        }

        // Trim trailing slashes
        var endIdx = trimmed.length - 1
        while (endIdx >= startIdx && trimmed[endIdx] == '/') {
            endIdx--
        }

        if (startIdx > endIdx) return null

        // Take the last meaningful segment if it looks like a path
        val lastSlash = trimmed.lastIndexOf('/', endIdx)
        if (lastSlash >= startIdx) {
            startIdx = lastSlash + 1
        }

        // Process characters for camelCase, snake_case, kebab-case, and normalize whitespace
        val sb = StringBuilder(endIdx - startIdx + 10)
        var prevChar = ' '

        for (i in startIdx..endIdx) {
            val c = trimmed[i]

            val isSeparator = c == '_' || c == '-' || c.isWhitespace()

            if (isSeparator) {
                if (prevChar != ' ') {
                    sb.append(' ')
                    prevChar = ' '
                }
            } else {
                // Split camelCase
                if (prevChar in 'a'..'z' && c in 'A'..'Z') {
                    sb.append(' ')
                }
                sb.append(c)
                prevChar = c
            }
        }

        val cleaned = sb.toString().trim()
        if (cleaned.isEmpty()) return null

        // Reject things that look like IDs
        var hexChars = 0
        var digitChars = 0
        var totalChars = 0

        for (i in 0 until cleaned.length) {
            val c = cleaned[i]
            if (c == ' ') continue
            totalChars++
            if (c in '0'..'9') {
                digitChars++
                hexChars++
            } else if (c in 'a'..'f' || c in 'A'..'F') {
                hexChars++
            }
        }

        if (totalChars >= 8 && hexChars == totalChars) return null
        if (totalChars > 0 && digitChars == totalChars) return null

        return cleaned.lowercase()
    }
}
