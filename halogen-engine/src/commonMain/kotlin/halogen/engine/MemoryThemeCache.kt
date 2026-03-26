package halogen.engine

import halogen.HalogenThemeSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory LRU cache for [HalogenThemeSpec] instances.
 *
 * Uses a [MutableMap] with manual LRU eviction tracking via access timestamps.
 */
internal class MemoryThemeCache(private val maxEntries: Int = 20) : ThemeCache {

    internal data class Entry(
        val spec: HalogenThemeSpec,
        val source: ThemeSource,
        val createdAt: Long,
        var lastAccessedAt: Long,
        val sizeBytes: Int,
    )

    private val mutex = Mutex()
    private val store = mutableMapOf<String, Entry>()

    private val _changes = MutableSharedFlow<CacheEvent>(extraBufferCapacity = 64)

    override suspend fun get(key: String): HalogenThemeSpec? = mutex.withLock {
        val entry = store[key] ?: return@withLock null
        entry.lastAccessedAt = currentTimeMillis()
        entry.spec
    }

    override suspend fun put(key: String, spec: HalogenThemeSpec, source: ThemeSource): Unit = mutex.withLock {
        val now = currentTimeMillis()

        store[key] = Entry(
            spec = spec,
            source = source,
            createdAt = now,
            lastAccessedAt = now,
            sizeBytes = 0,
        )

        // Evict LRU entries if over capacity
        while (store.size > maxEntries) {
            val eldest = store.entries.minByOrNull { it.value.lastAccessedAt }
            if (eldest != null) {
                store.remove(eldest.key)
                _changes.tryEmit(CacheEvent.Evicted(eldest.key))
            } else {
                break
            }
        }

        _changes.tryEmit(CacheEvent.Inserted(key, source))
    }

    override suspend fun contains(key: String): Boolean = mutex.withLock {
        store.containsKey(key)
    }

    override suspend fun evict(key: String): Unit = mutex.withLock {
        if (store.remove(key) != null) {
            _changes.tryEmit(CacheEvent.Evicted(key))
        }
    }

    override suspend fun evict(keys: Set<String>): Unit = mutex.withLock {
        val removed = mutableSetOf<String>()
        for (k in keys) {
            if (store.remove(k) != null) {
                removed.add(k)
            }
        }
        if (removed.isNotEmpty()) {
            _changes.tryEmit(CacheEvent.EvictedBatch(removed))
        }
    }

    override suspend fun clear(): Unit = mutex.withLock {
        store.clear()
        _changes.tryEmit(CacheEvent.Cleared)
    }

    override suspend fun keys(): Set<String> = mutex.withLock {
        store.keys.toSet()
    }

    override suspend fun size(): Int = mutex.withLock {
        store.size
    }

    override suspend fun entries(): List<ThemeCacheEntry> = mutex.withLock {
        store.map { (key, entry) ->
            ThemeCacheEntry(
                key = key,
                source = entry.source,
                createdAt = entry.createdAt,
                lastAccessedAt = entry.lastAccessedAt,
                sizeBytes = entry.sizeBytes,
            )
        }
    }

    override fun observeChanges(): Flow<CacheEvent> = _changes.asSharedFlow()
}
