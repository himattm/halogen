package halogen.engine

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import halogen.engine.db.HalogenDatabase
import platform.Foundation.NSHomeDirectory

internal fun createHalogenDatabase(): HalogenDatabase {
    val dbPath = "${NSHomeDirectory()}/halogen_themes.db"
    return Room.databaseBuilder<HalogenDatabase>(name = dbPath)
        .setDriver(BundledSQLiteDriver())
        .build()
}
