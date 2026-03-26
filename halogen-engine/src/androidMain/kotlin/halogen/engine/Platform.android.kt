package halogen.engine

import android.content.Context
import androidx.room.Room
import halogen.engine.db.HalogenDatabase

internal fun createHalogenDatabase(context: Context): HalogenDatabase {
    return Room.databaseBuilder(
        context,
        HalogenDatabase::class.java,
        "halogen_themes.db",
    ).build()
}
