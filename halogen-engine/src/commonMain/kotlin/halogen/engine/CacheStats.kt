package halogen.engine

/**
 * Snapshot of cache statistics for diagnostics and monitoring.
 */
public data class CacheStats(
    val totalEntries: Int = 0,
    val totalSizeBytes: Long = 0,
    val hitCount: Long = 0,
    val missCount: Long = 0,
    val hitRate: Float = 0f,
    val entriesBySource: Map<ThemeSource, Int> = emptyMap(),
)
