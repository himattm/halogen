package halogen.cache.room

import halogen.engine.ThemeCache

public object HalogenRoomCache {

    private const val DEFAULT_DB_NAME = "halogen_themes.db"

    public fun create(
        dbName: String = DEFAULT_DB_NAME,
        config: RoomThemeCacheConfig = RoomThemeCacheConfig.Default,
    ): ThemeCache {
        val database = buildHalogenDatabase(dbName)
        return RoomThemeCache(database.themeDao(), config)
    }
}
