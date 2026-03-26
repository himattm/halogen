package halogen.engine.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(entities = [ThemeEntity::class], version = 1)
@ConstructedBy(HalogenDatabaseConstructor::class)
internal abstract class HalogenDatabase : RoomDatabase() {
    abstract fun themeDao(): ThemeDao
}

// The Room KMP compiler generates the actual implementation.
@Suppress("NO_ACTUAL_FOR_EXPECT", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal expect object HalogenDatabaseConstructor : RoomDatabaseConstructor<HalogenDatabase>
