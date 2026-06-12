package halogen.image

/** Parse a hex color like "#1A73E8" to ARGB int. */
internal fun parseHex(hex: String): Int {
    // Bolt Optimization: Manual char iteration avoids string allocations.
    var rgb = 0
    val prefixLength = if (hex.startsWith("#")) 1 else 0
    for (i in prefixLength until hex.length) {
        val c = hex[i]
        val value = when {
            c in '0'..'9' -> c - '0'
            c in 'a'..'f' -> c - 'a' + 10
            c in 'A'..'F' -> c - 'A' + 10
            else -> throw IllegalArgumentException("Invalid hex character: $c in color $hex")
        }
        rgb = (rgb shl 4) or value
    }
    return rgb or (0xFF shl 24).toInt()
}
