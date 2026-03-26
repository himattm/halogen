package halogen.cache.room.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "halogen_themes")
internal data class ThemeEntity(
    @PrimaryKey val key: String,
    val spec: String,
    val createdAt: Long,
    val lastAccessedAt: Long,
    val source: String,
    val sizeBytes: Int,
)
