package halogen.engine

import halogen.HalogenThemeSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * A no-op [ThemeCache] implementation. All gets return null, all writes are ignored.
 */
internal class NoOpThemeCache : ThemeCache {

    private val _changes = MutableSharedFlow<CacheEvent>(extraBufferCapacity = 16)

    override suspend fun get(key: String): HalogenThemeSpec? = null

    override suspend fun put(key: String, spec: HalogenThemeSpec, source: ThemeSource) {
        // no-op
    }

    override suspend fun contains(key: String): Boolean = false

    override suspend fun evict(key: String) {
        // no-op
    }

    override suspend fun evict(keys: Set<String>) {
        // no-op
    }

    override suspend fun clear() {
        // no-op
    }

    override suspend fun keys(): Set<String> = emptySet()

    override suspend fun size(): Int = 0

    override suspend fun entries(): List<ThemeCacheEntry> = emptyList()

    override fun observeChanges(): Flow<CacheEvent> = _changes.asSharedFlow()
}
