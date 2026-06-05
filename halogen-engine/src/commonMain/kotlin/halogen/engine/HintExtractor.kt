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
        var start = 0
        val len = trimmed.length

        // Strip common prefixes
        if (trimmed.startsWith("/r/")) start = 3
        else if (trimmed.startsWith("/category/")) start = 10
        else if (trimmed.startsWith("/topic/")) start = 7
        else if (trimmed.startsWith("/") || trimmed.startsWith("#")) start = 1

        var end = len
        while (start < end && trimmed[start] == '/') start++
        while (start < end && trimmed[end - 1] == '/') end--

        // Take the last meaningful segment if it looks like a path
        val lastSlash = trimmed.lastIndexOf('/', end - 1)
        if (lastSlash >= start) {
            start = lastSlash + 1
        }

        if (start >= end) return null

        val sb = StringBuilder()
        var hasValidChar = false
        var isAllHex = true
        var isAllDigit = true
        var charCount = 0
        var spacePending = false
        var prevWasLower = false

        for (i in start until end) {
            val c = trimmed[i]
            if (c == '_' || c == '-' || c.isWhitespace()) {
                if (hasValidChar) {
                    spacePending = true
                }
                prevWasLower = false
            } else {
                val isUpper = c.isUpperCase()
                val isLower = c.isLowerCase()

                // Split camelCase
                if (prevWasLower && isUpper) {
                    spacePending = true
                }

                if (spacePending && hasValidChar) {
                    sb.append(' ')
                    spacePending = false
                }

                sb.append(c.lowercaseChar())
                hasValidChar = true
                prevWasLower = isLower
                charCount++

                // Validation checks
                if (c !in '0'..'9') {
                    isAllDigit = false
                    if (c !in 'a'..'f' && c !in 'A'..'F') {
                        isAllHex = false
                    }
                }
            }
        }

        if (!hasValidChar) return null
        if (isAllDigit) return null
        if (isAllHex && charCount >= 8) return null

        return sb.toString()
    }
}
