package halogen.engine

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import halogen.engine.db.HalogenDatabase

internal fun createHalogenDatabase(path: String = "halogen_themes.db"): HalogenDatabase {
    return Room.databaseBuilder<HalogenDatabase>(name = path)
        .setDriver(BundledSQLiteDriver())
        .build()
}
