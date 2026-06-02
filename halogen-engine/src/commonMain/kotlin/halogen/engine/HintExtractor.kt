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

    private val PREFIXES = arrayOf("/r/", "/category/", "/topic/", "/", "#")

    fun extract(key: String): String? {
        if (key.isBlank()) return null

        // Strip common prefixes
        var start = 0
        val trimmedKey = key.trim()
        for (prefix in PREFIXES) {
            if (trimmedKey.startsWith(prefix)) {
                start = prefix.length
                break
            }
        }

        var cleaned = trimmedKey.substring(start).trim('/')

        // Take the last meaningful segment if it looks like a path
        val lastSlash = cleaned.lastIndexOf('/')
        if (lastSlash != -1) {
            cleaned = cleaned.substring(lastSlash + 1)
        }

        // Single pass for camelCase, snake_case, kebab-case, and whitespace
        val sb = StringBuilder(cleaned.length + 5)
        var lastWasSpace = true
        var prevChar: Char? = null

        for (i in cleaned.indices) {
            val c = cleaned[i]
            if (c == '_' || c == '-' || c.isWhitespace()) {
                if (!lastWasSpace) {
                    sb.append(' ')
                    lastWasSpace = true
                }
            } else {
                // Camel case detection
                if (prevChar != null && prevChar in 'a'..'z' && c in 'A'..'Z') {
                    if (!lastWasSpace) {
                        sb.append(' ')
                    }
                }
                sb.append(c)
                lastWasSpace = false
            }
            prevChar = c
        }

        val result = sb.toString().trim()
        if (result.isBlank()) return null

        // Reject things that look like IDs (numeric only or 8+ hex chars)
        var hexCount = 0
        var isNumericOnly = true
        var hasNonHex = false
        var charCount = 0
        for (i in result.indices) {
            val c = result[i]
            if (c.isWhitespace()) continue
            charCount++
            if (c !in '0'..'9') isNumericOnly = false
            if (c in '0'..'9' || c in 'A'..'F' || c in 'a'..'f') {
                hexCount++
            } else {
                hasNonHex = true
            }
        }

        if (charCount == 0) return null

        if (isNumericOnly) return null
        if (!hasNonHex && hexCount >= 8) return null

        return result.lowercase()
    }
}
