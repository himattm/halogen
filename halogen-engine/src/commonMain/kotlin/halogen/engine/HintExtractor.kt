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

        // Strip common prefixes
        var cleaned = key.trim()
        if (cleaned.startsWith("/r/")) {
            cleaned = cleaned.substring(3)
        } else if (cleaned.startsWith("/category/")) {
            cleaned = cleaned.substring(10)
        } else if (cleaned.startsWith("/topic/")) {
            cleaned = cleaned.substring(7)
        } else if (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1)
        } else if (cleaned.startsWith("#")) {
            cleaned = cleaned.substring(1)
        }

        // Remove leading/trailing slashes
        var start = 0
        var end = cleaned.length - 1
        while (start <= end && cleaned[start] == '/') start++
        while (end >= start && cleaned[end] == '/') end--
        if (start > end) return null
        cleaned = cleaned.substring(start, end + 1)

        // Take the last meaningful segment if it looks like a path
        val lastSlashIndex = cleaned.lastIndexOf('/')
        if (lastSlashIndex >= 0) {
            cleaned = cleaned.substring(lastSlashIndex + 1)
        }

        val sb = StringBuilder(cleaned.length + 4) // Some extra space for added spaces
        var lastWasSpace = true

        for (i in cleaned.indices) {
            val c = cleaned[i]
            if (c == '_' || c == '-' || c.isWhitespace()) {
                if (!lastWasSpace) {
                    sb.append(' ')
                    lastWasSpace = true
                }
            } else {
                if (i > 0 && c.isUpperCase() && cleaned[i - 1].isLowerCase()) {
                    if (!lastWasSpace) {
                        sb.append(' ')
                    }
                }
                sb.append(c.lowercaseChar())
                lastWasSpace = false
            }
        }

        var result = sb.toString()
        if (result.endsWith(" ")) {
            result = result.substring(0, result.length - 1)
        }

        if (result.isBlank()) return null

        // Reject things that look like IDs
        var noSpacesLen = 0
        var allNumeric = true
        var allHex = true
        for (i in result.indices) {
            val c = result[i]
            if (c != ' ') {
                noSpacesLen++
                if (c !in '0'..'9') allNumeric = false
                if (c !in '0'..'9' && c !in 'a'..'f') allHex = false
            }
        }

        if (noSpacesLen > 0 && allNumeric) return null
        if (noSpacesLen >= 8 && allHex) return null

        return result
    }
}
