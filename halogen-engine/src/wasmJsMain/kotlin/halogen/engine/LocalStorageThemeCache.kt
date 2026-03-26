package halogen.engine

import halogen.HalogenThemeSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * A [ThemeCache] backed by browser localStorage for wasmJs targets.
 *
 * This is currently a thin wrapper around [MemoryThemeCache] because
 * browser localStorage interop in Kotlin/Wasm is still maturing.
 * A full localStorage implementation will replace this when the APIs stabilize.
 *
 * @param prefix Key prefix for localStorage entries (reserved for future use).
 */
public class LocalStorageThemeCache(
    @Suppress("UNUSED_PARAMETER") private val prefix: String = "halogen_",
) : ThemeCache {

    private val delegate: ThemeCache = MemoryThemeCache()

    override suspend fun get(key: String): HalogenThemeSpec? = delegate.get(key)

    override suspend fun put(key: String, spec: HalogenThemeSpec, source: ThemeSource) {
        delegate.put(key, spec, source)
    }

    override suspend fun contains(key: String): Boolean = delegate.contains(key)

    override suspend fun evict(key: String) {
        delegate.evict(key)
    }

    override suspend fun evict(keys: Set<String>) {
        delegate.evict(keys)
    }

    override suspend fun clear() {
        delegate.clear()
    }

    override suspend fun keys(): Set<String> = delegate.keys()

    override suspend fun size(): Int = delegate.size()

    override suspend fun entries(): List<ThemeCacheEntry> = delegate.entries()

    override fun observeChanges(): Flow<CacheEvent> = delegate.observeChanges()
}
