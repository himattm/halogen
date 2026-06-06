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
        var start = when {
            trimmed.startsWith("/r/") -> 3
            trimmed.startsWith("/category/") -> 10
            trimmed.startsWith("/topic/") -> 7
            trimmed.startsWith("/") -> 1
            trimmed.startsWith("#") -> 1
            else -> 0
        }

        // Remove leading/trailing slashes
        var end = trimmed.length
        while (start < end && trimmed[start] == '/') start++
        while (end > start && trimmed[end - 1] == '/') end--
        if (start >= end) return null

        // Take the last meaningful segment if it looks like a path
        val lastSlash = trimmed.lastIndexOf('/', end - 1)
        if (lastSlash >= start) {
            start = lastSlash + 1
        }

        val builder = StringBuilder(end - start)
        var previousOutput = ' '
        var previousOriginal = ' '

        for (index in start until end) {
            val char = trimmed[index]

            if (char == '_' || char == '-' || char.isWhitespace()) {
                if (previousOutput != ' ') {
                    builder.append(' ')
                    previousOutput = ' '
                }
            } else {
                if (previousOriginal in 'a'..'z' && char in 'A'..'Z' && previousOutput != ' ') {
                    builder.append(' ')
                }
                builder.append(char)
                previousOutput = char
            }

            previousOriginal = char
        }

        val cleaned = builder.toString().trim()

        if (cleaned.isBlank()) return null

        // Reject things that look like IDs
        if (looksLikeId(cleaned)) return null

        return cleaned.lowercase()
    }

    private fun looksLikeId(value: String): Boolean {
        var length = 0
        var allNumeric = true
        var allHex = true

        for (char in value) {
            if (char == ' ') continue

            length++
            if (char !in '0'..'9') allNumeric = false
            if (char !in '0'..'9' && char !in 'a'..'f' && char !in 'A'..'F') allHex = false

            if (!allNumeric && !allHex) return false
        }

        return length > 0 && (allNumeric || (length >= 8 && allHex))
    }
}
