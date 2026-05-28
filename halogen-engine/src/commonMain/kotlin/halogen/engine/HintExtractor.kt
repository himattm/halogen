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

    private fun isIdOrNumeric(value: String): Boolean {
        if (value.isEmpty()) return false
        var allNumeric = true
        var isHex = value.length >= 8
        for (i in value.indices) {
            val c = value[i]
            if (c !in '0'..'9') {
                allNumeric = false
            }
            if (!(c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F')) {
                isHex = false
            }
            if (!allNumeric && !isHex) return false
        }
        return allNumeric || isHex
    }

    fun extract(key: String): String? {
        if (key.isBlank()) return null

        val trimmed = key.trim()
        var startIdx = 0
        if (trimmed.startsWith("/r/")) startIdx = 3
        else if (trimmed.startsWith("/category/")) startIdx = 10
        else if (trimmed.startsWith("/topic/")) startIdx = 7
        else if (trimmed.startsWith("/")) startIdx = 1
        else if (trimmed.startsWith("#")) startIdx = 1

        var cleaned = trimmed.substring(startIdx).trim('/')

        if ('/' in cleaned) {
            cleaned = cleaned.substringAfterLast('/')
        }

        val sb = StringBuilder()
        var lastWasSpace = true
        for (i in cleaned.indices) {
            val c = cleaned[i]
            if (c == '_' || c == '-' || c.isWhitespace()) {
                if (!lastWasSpace) {
                    sb.append(' ')
                    lastWasSpace = true
                }
            } else {
                if (c.isUpperCase() && i > 0 && cleaned[i - 1].isLowerCase()) {
                    if (!lastWasSpace) {
                        sb.append(' ')
                    }
                }
                sb.append(c.lowercaseChar())
                lastWasSpace = false
            }
        }

        val result = sb.toString().trim()
        if (result.isEmpty()) return null

        val noSpaces = result.replace(" ", "")
        if (isIdOrNumeric(noSpaces)) return null

        return result
    }
}
