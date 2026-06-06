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

    private fun stripPrefix(key: String): String {
        for (prefix in PREFIXES) {
            if (key.startsWith(prefix)) {
                return key.substring(prefix.length)
            }
        }
        return key
    }

    private fun isHexId(str: String): Boolean {
        if (str.length < 8) return false
        for (i in 0 until str.length) {
            val c = str[i]
            if (!(c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F')) return false
        }
        return true
    }

    private fun isNumeric(str: String): Boolean {
        if (str.isEmpty()) return false
        for (i in 0 until str.length) {
            if (str[i] !in '0'..'9') return false
        }
        return true
    }

    fun extract(key: String): String? {
        if (key.isBlank()) return null

        var cleaned = stripPrefix(key.trim())

        var startIdx = 0
        while (startIdx < cleaned.length && cleaned[startIdx] == '/') {
            startIdx++
        }
        var endIdx = cleaned.length - 1
        while (endIdx >= startIdx && cleaned[endIdx] == '/') {
            endIdx--
        }
        if (startIdx > endIdx) return null
        cleaned = cleaned.substring(startIdx, endIdx + 1)

        val lastSlash = cleaned.lastIndexOf('/')
        if (lastSlash != -1) {
            cleaned = cleaned.substring(lastSlash + 1)
        }

        val sb = StringBuilder(cleaned.length + 5)
        var lastChar = ' '
        for (i in 0 until cleaned.length) {
            val c = cleaned[i]
            val isUpper = c in 'A'..'Z'
            val isSymbolOrWhitespace = c == '_' || c == '-' || c.isWhitespace()

            if (isUpper && lastChar in 'a'..'z') {
                sb.append(' ')
            }

            if (isSymbolOrWhitespace) {
                if (lastChar != ' ') {
                    sb.append(' ')
                    lastChar = ' '
                }
            } else {
                sb.append(c)
                lastChar = c
            }
        }

        cleaned = sb.toString().trim()

        if (cleaned.isBlank()) return null

        val noSpaces = StringBuilder(cleaned.length)
        for (i in 0 until cleaned.length) {
            if (cleaned[i] != ' ') noSpaces.append(cleaned[i])
        }
        val noSpacesStr = noSpaces.toString()

        if (isHexId(noSpacesStr)) return null
        if (isNumeric(noSpacesStr)) return null

        return cleaned.lowercase()
    }
}
