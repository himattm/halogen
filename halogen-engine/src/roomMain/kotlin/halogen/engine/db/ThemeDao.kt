package halogen.engine.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
internal interface ThemeDao {
    @Query("SELECT * FROM halogen_themes WHERE `key` = :key")
    suspend fun getByKey(key: String): ThemeEntity?

    @Upsert
    suspend fun upsert(entity: ThemeEntity)

    @Query("DELETE FROM halogen_themes WHERE `key` = :key")
    suspend fun delete(key: String)

    @Query("DELETE FROM halogen_themes")
    suspend fun deleteAll()

    @Query("SELECT `key` FROM halogen_themes")
    suspend fun getAllKeys(): List<String>

    @Query("SELECT * FROM halogen_themes")
    suspend fun getAll(): List<ThemeEntity>

    @Query("DELETE FROM halogen_themes WHERE createdAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("SELECT COUNT(*) FROM halogen_themes")
    suspend fun count(): Int
}
