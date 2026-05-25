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
        if (cleaned.startsWith("/r/")) cleaned = cleaned.substring(3)
        else if (cleaned.startsWith("/category/")) cleaned = cleaned.substring(10)
        else if (cleaned.startsWith("/topic/")) cleaned = cleaned.substring(7)
        else if (cleaned.startsWith("/")) cleaned = cleaned.substring(1)
        else if (cleaned.startsWith("#")) cleaned = cleaned.substring(1)

        cleaned = cleaned.trim('/')
        if ('/' in cleaned) {
            cleaned = cleaned.substringAfterLast('/')
        }

        val builder = StringBuilder(cleaned.length + 5)
        var lastAdded = ' '
        var prevOriginal = ' '
        for (i in cleaned.indices) {
            val c = cleaned[i]

            val isDelimiter = c == '_' || c == '-' || c.isWhitespace()
            if (isDelimiter) {
                if (lastAdded != ' ') {
                    builder.append(' ')
                    lastAdded = ' '
                }
            } else {
                if (c in 'A'..'Z' && prevOriginal in 'a'..'z') {
                    if (lastAdded != ' ') {
                        builder.append(' ')
                    }
                }
                val lower = c.lowercaseChar()
                builder.append(lower)
                lastAdded = lower
            }
            prevOriginal = c
        }

        val finalStr = builder.toString().trim()
        if (finalStr.isEmpty()) return null

        var allDigits = true
        var allHex = true
        var charCount = 0

        for (i in finalStr.indices) {
            val c = finalStr[i]
            if (c == ' ') continue
            charCount++

            if (c !in '0'..'9') {
                allDigits = false
                if (c !in 'a'..'f' && c !in 'A'..'F') {
                    allHex = false
                }
            }
        }

        if (charCount > 0 && allDigits) return null
        if (charCount >= 8 && allHex) return null

        return finalStr
    }
}
