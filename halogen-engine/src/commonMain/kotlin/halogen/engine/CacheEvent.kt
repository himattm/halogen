package halogen.engine

/**
 * Events emitted by a [ThemeCache] when its contents change.
 * Observe via [ThemeCache.observeChanges].
 */
public sealed class CacheEvent {
    /** A theme was inserted (or replaced) for [key] from [source]. */
    public data class Inserted(val key: String, val source: ThemeSource) : CacheEvent()

    /** A single entry was evicted by [key]. */
    public data class Evicted(val key: String) : CacheEvent()

    /** Multiple entries were evicted in a single operation. */
    public data class EvictedBatch(val keys: Set<String>) : CacheEvent()

    /** All entries were removed from the cache. */
    public data object Cleared : CacheEvent()
}
