package halogen.cache.room

import kotlin.time.Duration

/**
 * Configuration for [RoomThemeCache].
 *
 * @param maxEntries Maximum number of themes to store. 0 = unlimited.
 * @param maxAge Maximum age before auto-eviction on read. null = no expiry.
 */
public data class RoomThemeCacheConfig(
    val maxEntries: Int = 0,
    val maxAge: Duration? = null,
) {
    public companion object {
        public val Default: RoomThemeCacheConfig = RoomThemeCacheConfig()

        public fun withLimit(maxEntries: Int): RoomThemeCacheConfig =
            RoomThemeCacheConfig(maxEntries = maxEntries)

        public fun withExpiry(maxAge: Duration): RoomThemeCacheConfig =
            RoomThemeCacheConfig(maxAge = maxAge)
    }
}
