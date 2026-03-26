package halogen.engine

/**
 * Metadata for a single entry in a [ThemeCache].
 * Does not contain the spec itself — use [ThemeCache.get] to retrieve it.
 */
public data class ThemeCacheEntry(
    val key: String,
    val source: ThemeSource,
    val createdAt: Long,
    val lastAccessedAt: Long,
    val sizeBytes: Int,
)
