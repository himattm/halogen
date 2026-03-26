package halogen.cache.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import halogen.cache.room.db.HalogenDatabase
import platform.Foundation.NSHomeDirectory

internal actual fun buildHalogenDatabase(name: String): HalogenDatabase =
    Room.databaseBuilder<HalogenDatabase>(name = NSHomeDirectory() + "/$name")
        .setDriver(BundledSQLiteDriver())
        .build()
