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

    // ⚡ Bolt Performance Optimization:
    // Replaced multiple string-manipulating regular expressions (lookarounds for camel case,
    // format validation, whitespace normalization) with a single manual character iteration
    // loop and a StringBuilder. This avoids significant compilation and backtracking overhead
    // from the Regex state machine in this hot path.
    fun extract(key: String): String? {
        if (key.isBlank()) return null

        // Strip common prefixes
        var start = 0
        var end = key.length - 1

        while (start <= end && key[start].isWhitespace()) start++
        while (end >= start && key[end].isWhitespace()) end--

        if (start > end) return null

        var tempStr = key.substring(start, end + 1)
        if (tempStr.startsWith("/r/")) {
            start += 3
        } else if (tempStr.startsWith("/category/")) {
            start += 10
        } else if (tempStr.startsWith("/topic/")) {
            start += 7
        } else if (tempStr.startsWith("/") || tempStr.startsWith("#")) {
            start += 1
        }

        // Remove leading/trailing slashes
        while (start <= end && key[start] == '/') start++
        while (end >= start && key[end] == '/') end--

        if (start > end) return null

        // Take the last meaningful segment if it looks like a path
        val lastSlash = key.lastIndexOf('/', end)
        if (lastSlash >= start) {
            start = lastSlash + 1
        }

        // Split camelCase, snake_case, kebab-case, whitespace
        val sb = StringBuilder()
        var prevIsLower = false
        var prevIsSpace = true

        for (i in start..end) {
            val c = key[i]

            if (c == '_' || c == '-' || c.isWhitespace()) {
                if (!prevIsSpace) {
                    sb.append(' ')
                    prevIsSpace = true
                }
                prevIsLower = false
            } else {
                val isUpper = c.isUpperCase()
                if (isUpper && prevIsLower) {
                    if (!prevIsSpace) {
                        sb.append(' ')
                    }
                }

                sb.append(c.lowercaseChar())
                prevIsSpace = false
                prevIsLower = c.isLowerCase()
            }
        }

        var cleaned = sb.toString()
        if (cleaned.endsWith(" ")) {
            cleaned = cleaned.substring(0, cleaned.length - 1)
        }
        if (cleaned.isEmpty()) return null

        // Reject things that look like IDs
        var isAllHex = true
        var isAllDigits = true
        var lengthWithoutSpaces = 0

        for (i in 0 until cleaned.length) {
            val c = cleaned[i]
            if (c != ' ') {
                lengthWithoutSpaces++
                if (c in '0'..'9') {
                    // is digit
                } else {
                    isAllDigits = false
                    if (c !in 'a'..'f') {
                        isAllHex = false
                    }
                }
            }
        }

        if (lengthWithoutSpaces == 0) return null
        if (isAllDigits) return null
        if (isAllHex && lengthWithoutSpaces >= 8) return null

        return cleaned
    }
}
