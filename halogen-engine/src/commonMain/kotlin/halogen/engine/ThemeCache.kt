package halogen.engine

import halogen.HalogenThemeSpec
import kotlinx.coroutines.flow.Flow

/**
 * Cache abstraction for storing and retrieving [HalogenThemeSpec] instances.
 *
 * Implementations must be thread-safe. Built-in options are available via
 * [HalogenCache.memory] and [HalogenCache.none].
 */
public interface ThemeCache {
    /** Retrieve a cached spec by [key], or `null` if not present. */
    public suspend fun get(key: String): HalogenThemeSpec?

    /** Store a [spec] under [key] with an associated [source]. */
    public suspend fun put(key: String, spec: HalogenThemeSpec, source: ThemeSource = ThemeSource.LLM)

    /** Check whether [key] exists in the cache. */
    public suspend fun contains(key: String): Boolean

    /** Remove the entry for [key]. */
    public suspend fun evict(key: String)

    /** Remove entries for all given [keys]. */
    public suspend fun evict(keys: Set<String>)

    /** Remove all entries from the cache. */
    public suspend fun clear()

    /** Return all keys currently in the cache. */
    public suspend fun keys(): Set<String>

    /** Return the number of entries in the cache. */
    public suspend fun size(): Int

    /** Return metadata for all cached entries. */
    public suspend fun entries(): List<ThemeCacheEntry>

    /** Observe cache mutations as a [Flow] of [CacheEvent]s. */
    public fun observeChanges(): Flow<CacheEvent>
}
