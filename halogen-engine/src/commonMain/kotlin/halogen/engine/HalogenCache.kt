package halogen.engine

/**
 * Factory for [ThemeCache] implementations.
 */
public object HalogenCache {
    /**
     * Create an in-memory LRU cache.
     *
     * @param maxEntries Maximum number of cached themes before LRU eviction. Default 20.
     */
    public fun memory(maxEntries: Int = 20): ThemeCache = MemoryThemeCache(maxEntries)

    /**
     * Create a no-op cache that stores nothing.
     */
    public fun none(): ThemeCache = NoOpThemeCache()

    // room() will be added when Room-backed ThemeCache implementation is ready
}
