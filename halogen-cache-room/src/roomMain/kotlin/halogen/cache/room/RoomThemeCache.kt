package halogen.cache.room

import halogen.HalogenThemeSpec
import halogen.cache.room.db.ThemeDao
import halogen.cache.room.db.ThemeEntity
import halogen.engine.CacheEvent
import halogen.engine.ThemeCache
import halogen.engine.ThemeCacheEntry
import halogen.engine.ThemeSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.json.Json

public class RoomThemeCache internal constructor(
    private val dao: ThemeDao,
    private val config: RoomThemeCacheConfig = RoomThemeCacheConfig.Default,
) : ThemeCache {

    private val json = Json { ignoreUnknownKeys = true }
    private val _changes = MutableSharedFlow<CacheEvent>(extraBufferCapacity = 64)

    override suspend fun get(key: String): HalogenThemeSpec? {
        val entity = dao.getByKey(key) ?: return null

        // Check max age expiry
        if (config.maxAge != null) {
            val age = currentTimeMillis() - entity.createdAt
            if (age > config.maxAge.inWholeMilliseconds) {
                dao.delete(key)
                _changes.tryEmit(CacheEvent.Evicted(key))
                return null
            }
        }

        // Update last accessed time
        dao.upsert(entity.copy(lastAccessedAt = currentTimeMillis()))
        return json.decodeFromString<HalogenThemeSpec>(entity.spec)
    }

    override suspend fun put(key: String, spec: HalogenThemeSpec, source: ThemeSource) {
        val serialized = json.encodeToString(HalogenThemeSpec.serializer(), spec)
        val now = currentTimeMillis()
        dao.upsert(
            ThemeEntity(
                key = key,
                spec = serialized,
                createdAt = now,
                lastAccessedAt = now,
                source = source.name,
                sizeBytes = serialized.encodeToByteArray().size,
            )
        )
        _changes.tryEmit(CacheEvent.Inserted(key, source))

        // Enforce max entries by evicting oldest
        if (config.maxEntries > 0) {
            val count = dao.count()
            if (count > config.maxEntries) {
                val all = dao.getAll().sortedBy { it.lastAccessedAt }
                val toEvict = all.take(count - config.maxEntries)
                toEvict.forEach { dao.delete(it.key) }
                if (toEvict.size == 1) {
                    _changes.tryEmit(CacheEvent.Evicted(toEvict.first().key))
                } else if (toEvict.size > 1) {
                    _changes.tryEmit(CacheEvent.EvictedBatch(toEvict.map { it.key }.toSet()))
                }
            }
        }
    }

    override suspend fun contains(key: String): Boolean =
        dao.getByKey(key) != null

    override suspend fun evict(key: String) {
        dao.delete(key)
        _changes.tryEmit(CacheEvent.Evicted(key))
    }

    override suspend fun evict(keys: Set<String>) {
        keys.forEach { dao.delete(it) }
        _changes.tryEmit(CacheEvent.EvictedBatch(keys))
    }

    override suspend fun clear() {
        dao.deleteAll()
        _changes.tryEmit(CacheEvent.Cleared)
    }

    override suspend fun keys(): Set<String> =
        dao.getAllKeys().toSet()

    override suspend fun size(): Int =
        dao.count()

    override suspend fun entries(): List<ThemeCacheEntry> =
        dao.getAll().map { entity ->
            ThemeCacheEntry(
                key = entity.key,
                source = ThemeSource.valueOf(entity.source),
                createdAt = entity.createdAt,
                lastAccessedAt = entity.lastAccessedAt,
                sizeBytes = entity.sizeBytes,
            )
        }

    override fun observeChanges(): Flow<CacheEvent> = _changes

    /**
     * Evict all entries older than [cutoffMillis] (epoch time).
     */
    public suspend fun evictOlderThan(cutoffMillis: Long) {
        val keysBeforeEviction = dao.getAllKeys().toSet()
        dao.deleteOlderThan(cutoffMillis)
        val keysAfterEviction = dao.getAllKeys().toSet()
        val evictedKeys = keysBeforeEviction - keysAfterEviction
        if (evictedKeys.isNotEmpty()) {
            _changes.tryEmit(CacheEvent.EvictedBatch(evictedKeys))
        }
    }
}
