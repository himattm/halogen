package halogen.engine

/**
 * Factory for built-in [ThemeCache] implementations.
 *
 * For persistent caching, add the `halogen-cache-room` dependency
 * and use `HalogenRoomCache.create()`.
 */
public object HalogenCache {

    /**
     * In-memory LRU cache. Fast but does not survive process death.
     */
    public fun memory(maxEntries: Int = 20): ThemeCache = MemoryThemeCache(maxEntries)

    /**
     * No-op cache. Every resolve hits the LLM provider.
     */
    public fun none(): ThemeCache = NoOpThemeCache()
}
