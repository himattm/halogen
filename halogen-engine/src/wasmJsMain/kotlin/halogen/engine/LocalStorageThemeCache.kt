package halogen.engine

import halogen.HalogenThemeSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// ── JS interop (must be top-level for @JsFun) ──────────────────────────

@JsFun("(key) => window.localStorage.getItem(key)")
private external fun jsGetItem(key: JsString): JsString?

@JsFun("(key, value) => window.localStorage.setItem(key, value)")
private external fun jsSetItem(key: JsString, value: JsString)

@JsFun("(key) => window.localStorage.removeItem(key)")
private external fun jsRemoveItem(key: JsString)

/**
 * A [ThemeCache] backed by browser `localStorage` for wasmJs targets.
 *
 * Each entry is stored as a JSON envelope containing the serialized spec
 * and metadata. A manifest key tracks all active theme keys so we never
 * need to iterate the entire localStorage namespace.
 *
 * @param prefix Key prefix for localStorage entries (default `"halogen_"`).
 */
public class LocalStorageThemeCache(
    private val prefix: String = "halogen_",
) : ThemeCache {

    // ── Internal types ──────────────────────────────────────────────────

    @Serializable
    internal data class LocalStorageEntry(
        val spec: String,
        val source: String,
        val createdAt: Long,
        val lastAccessedAt: Long,
        val sizeBytes: Int,
    )

    // ── State ───────────────────────────────────────────────────────────

    private val _changes = MutableSharedFlow<CacheEvent>(extraBufferCapacity = 64)
    private val json = Json { ignoreUnknownKeys = true }
    private val manifestKey = "${prefix}__keys__"

    // Cache the manifest in-memory to prevent repeated JSON decoding on every operation.
    // Lazy load the manifest once, then keep it in sync.
    private val manifestCache: MutableSet<String> by lazy { loadManifest() }

    // ── Manifest helpers ────────────────────────────────────────────────

    private fun loadManifest(): MutableSet<String> {
        val raw = jsGetItem(manifestKey.toJsString())?.toString() ?: return mutableSetOf()
        return try {
            json.decodeFromString<Set<String>>(raw).toMutableSet()
        } catch (_: Exception) {
            mutableSetOf()
        }
    }

    private fun writeManifest(keys: Set<String>) {
        val encoded = json.encodeToString(keys)
        jsSetItem(manifestKey.toJsString(), encoded.toJsString())
    }

    // ── Entry helpers ───────────────────────────────────────────────────

    private fun readEntry(storageKey: String): LocalStorageEntry? {
        val raw = jsGetItem(storageKey.toJsString())?.toString() ?: return null
        return try {
            json.decodeFromString<LocalStorageEntry>(raw)
        } catch (_: Exception) {
            null
        }
    }

    private fun writeEntry(storageKey: String, entry: LocalStorageEntry) {
        val encoded = json.encodeToString(entry)
        jsSetItem(storageKey.toJsString(), encoded.toJsString())
    }

    private fun removeEntry(storageKey: String) {
        jsRemoveItem(storageKey.toJsString())
    }

    // ── ThemeCache implementation ───────────────────────────────────────

    override suspend fun get(key: String): HalogenThemeSpec? {
        val storageKey = "$prefix$key"
        val entry = readEntry(storageKey) ?: return null
        writeEntry(storageKey, entry.copy(lastAccessedAt = currentTimeMillis()))
        return try {
            HalogenThemeSpec.fromJson(entry.spec)
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun put(key: String, spec: HalogenThemeSpec, source: ThemeSource) {
        val specJson = HalogenThemeSpec.toJson(spec)
        val storageKey = "$prefix$key"
        val now = currentTimeMillis()
        val entry = LocalStorageEntry(
            spec = specJson,
            source = source.name,
            createdAt = now,
            lastAccessedAt = now,
            sizeBytes = specJson.encodeToByteArray().size,
        )
        writeEntry(storageKey, entry)
        manifestCache.add(key)
        writeManifest(manifestCache)
        _changes.tryEmit(CacheEvent.Inserted(key, source))
    }

    override suspend fun contains(key: String): Boolean {
        return readEntry("$prefix$key") != null
    }

    override suspend fun evict(key: String) {
        val storageKey = "$prefix$key"
        val existed = readEntry(storageKey) != null
        removeEntry(storageKey)
        manifestCache.remove(key)
        writeManifest(manifestCache)
        if (existed) {
            _changes.tryEmit(CacheEvent.Evicted(key))
        }
    }

    override suspend fun evict(keys: Set<String>) {
        val removed = mutableSetOf<String>()
        for (k in keys) {
            val storageKey = "$prefix$k"
            if (readEntry(storageKey) != null) {
                removeEntry(storageKey)
                manifestCache.remove(k)
                removed.add(k)
            }
        }
        writeManifest(manifestCache)
        if (removed.isNotEmpty()) {
            _changes.tryEmit(CacheEvent.EvictedBatch(removed))
        }
    }

    override suspend fun clear() {
        for (key in manifestCache) {
            removeEntry("$prefix$key")
        }
        manifestCache.clear()
        jsRemoveItem(manifestKey.toJsString())
        _changes.tryEmit(CacheEvent.Cleared)
    }

    override suspend fun keys(): Set<String> = manifestCache.toSet()

    override suspend fun size(): Int = manifestCache.size

    override suspend fun entries(): List<ThemeCacheEntry> {
        return manifestCache.mapNotNull { key ->
            val entry = readEntry("$prefix$key") ?: return@mapNotNull null
            ThemeCacheEntry(
                key = key,
                source = try {
                    ThemeSource.valueOf(entry.source)
                } catch (_: Exception) {
                    ThemeSource.MANUAL
                },
                createdAt = entry.createdAt,
                lastAccessedAt = entry.lastAccessedAt,
                sizeBytes = entry.sizeBytes,
            )
        }
    }

    override fun observeChanges(): Flow<CacheEvent> = _changes.asSharedFlow()
}
