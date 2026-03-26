package halogen.cache.room

import android.content.Context
import androidx.room.Room
import halogen.cache.room.db.HalogenDatabase
import halogen.engine.initHalogen

private var appContext: Context? = null

public fun HalogenRoomCache.initialize(context: Context) {
    appContext = context.applicationContext
    initHalogen(context)
}

internal actual fun buildHalogenDatabase(name: String): HalogenDatabase {
    val ctx = appContext ?: error(
        "HalogenRoomCache.initialize(context) or initializeRoomCache(context) must be called before creating a Room cache on Android."
    )
    return Room.databaseBuilder(ctx, HalogenDatabase::class.java, name).build()
}
