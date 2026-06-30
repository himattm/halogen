package halogen.image

/** Parse a hex color like "#1A73E8" to ARGB int. */
internal fun parseHex(hex: String): Int {
    val clean = hex.removePrefix("#")
    var rgb = 0
    for (i in clean.indices) {
        val c = clean[i]
        val v = when (c) {
            in '0'..'9' -> c - '0'
            in 'a'..'f' -> c - 'a' + 10
            in 'A'..'F' -> c - 'A' + 10
            else -> throw IllegalArgumentException("Invalid hex character: $c")
        }
        rgb = (rgb shl 4) or v
    }
    return rgb or (0xFF shl 24)
}
