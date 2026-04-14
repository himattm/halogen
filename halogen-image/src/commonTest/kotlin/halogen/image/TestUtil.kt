package halogen.image

/** Parse a hex color like "#1A73E8" to ARGB int. */
internal fun parseHex(hex: String): Int {
    val rgb = hex.removePrefix("#").toLong(16).toInt()
    return rgb or (0xFF shl 24).toInt()
}
