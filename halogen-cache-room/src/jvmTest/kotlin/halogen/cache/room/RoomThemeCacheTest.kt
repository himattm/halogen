package halogen.cache.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import halogen.HalogenThemeSpec
import halogen.cache.room.db.HalogenDatabase
import halogen.engine.CacheEvent
import halogen.engine.ThemeSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoomThemeCacheTest {

    private lateinit var database: HalogenDatabase
    private lateinit var cache: RoomThemeCache

    private val oceanSpec: HalogenThemeSpec by lazy {
        HalogenThemeSpec.fromJson(OCEAN_SPEC_JSON)
    }
    private val neonSpec: HalogenThemeSpec by lazy {
        HalogenThemeSpec.fromJson(NEON_SPEC_JSON)
    }

    @BeforeTest
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder<HalogenDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        cache = RoomThemeCache(database.themeDao())
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    // ── Basic CRUD ──────────────────────────────────────────────────────

    @Test
    fun get_returnsNull_whenEmpty() = runTest {
        assertNull(cache.get("nonexistent"))
    }

    @Test
    fun put_then_get_returnsSameSpec() = runTest {
        cache.put("ocean", oceanSpec)
        assertEquals(oceanSpec, cache.get("ocean"))
    }

    @Test
    fun put_then_get_roundTripsAllFields() = runTest {
        cache.put("neon", neonSpec, ThemeSource.SERVER)
        val retrieved = cache.get("neon")
        assertEquals(neonSpec, retrieved)
    }

    // ── contains ────────────────────────────────────────────────────────

    @Test
    fun contains_returnsFalse_whenKeyMissing() = runTest {
        assertFalse(cache.contains("missing"))
    }

    @Test
    fun contains_returnsTrue_afterPut() = runTest {
        cache.put("ocean", oceanSpec)
        assertTrue(cache.contains("ocean"))
    }

    // ── evict (single) ─────────────────────────────────────────────────

    @Test
    fun evict_removesEntry() = runTest {
        cache.put("ocean", oceanSpec)
        cache.evict("ocean")
        assertNull(cache.get("ocean"))
    }

    @Test
    fun evict_doesNotAffectOtherEntries() = runTest {
        cache.put("ocean", oceanSpec)
        cache.put("neon", neonSpec)
        cache.evict("ocean")
        assertEquals(neonSpec, cache.get("neon"))
    }

    // ── evict (batch) ──────────────────────────────────────────────────

    @Test
    fun evictBatch_removesAllSpecifiedKeys() = runTest {
        cache.put("ocean", oceanSpec)
        cache.put("neon", neonSpec)
        cache.evict(setOf("ocean", "neon"))
        assertEquals(0, cache.size())
    }

    @Test
    fun evictBatch_doesNotAffectUnspecifiedKeys() = runTest {
        cache.put("ocean", oceanSpec)
        cache.put("neon", neonSpec)
        cache.put("third", oceanSpec)
        cache.evict(setOf("ocean", "neon"))
        assertEquals(1, cache.size())
        assertTrue(cache.contains("third"))
    }

    // ── clear ───────────────────────────────────────────────────────────

    @Test
    fun clear_removesAllEntries() = runTest {
        cache.put("ocean", oceanSpec)
        cache.put("neon", neonSpec)
        cache.clear()
        assertEquals(0, cache.size())
        assertNull(cache.get("ocean"))
        assertNull(cache.get("neon"))
    }

    // ── keys ────────────────────────────────────────────────────────────

    @Test
    fun keys_returnsEmptySet_whenEmpty() = runTest {
        assertEquals(emptySet(), cache.keys())
    }

    @Test
    fun keys_returnsAllStoredKeys() = runTest {
        cache.put("ocean", oceanSpec)
        cache.put("neon", neonSpec)
        assertEquals(setOf("ocean", "neon"), cache.keys())
    }

    // ── size ────────────────────────────────────────────────────────────

    @Test
    fun size_returnsZero_whenEmpty() = runTest {
        assertEquals(0, cache.size())
    }

    @Test
    fun size_returnsCorrectCount() = runTest {
        cache.put("ocean", oceanSpec)
        assertEquals(1, cache.size())
        cache.put("neon", neonSpec)
        assertEquals(2, cache.size())
    }

    // ── entries ─────────────────────────────────────────────────────────

    @Test
    fun entries_returnsMetadataForAllEntries() = runTest {
        cache.put("ocean", oceanSpec, ThemeSource.LLM)
        cache.put("neon", neonSpec, ThemeSource.SERVER)
        val entries = cache.entries()
        assertEquals(2, entries.size)
        val keys = entries.map { it.key }.toSet()
        assertEquals(setOf("ocean", "neon"), keys)
        val sources = entries.associate { it.key to it.source }
        assertEquals(ThemeSource.LLM, sources["ocean"])
        assertEquals(ThemeSource.SERVER, sources["neon"])
    }

    @Test
    fun entries_containsCorrectSizeBytes() = runTest {
        cache.put("ocean", oceanSpec, ThemeSource.LLM)
        val entry = cache.entries().single()
        assertTrue(entry.sizeBytes > 0, "sizeBytes should be positive")
    }

    // ── Max entries eviction ────────────────────────────────────────────

    @Test
    fun maxEntries_evictsOldestWhenOverCapacity() = runTest {
        database.close()
        database = Room.inMemoryDatabaseBuilder<HalogenDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        val limitedCache = RoomThemeCache(
            dao = database.themeDao(),
            config = RoomThemeCacheConfig(maxEntries = 2),
        )

        limitedCache.put("first", oceanSpec)
        limitedCache.put("second", neonSpec)
        limitedCache.put("third", oceanSpec) // should evict "first"

        assertNull(limitedCache.get("first"), "Oldest entry should be evicted")
        assertEquals(2, limitedCache.size())
        assertTrue(limitedCache.contains("second"))
        assertTrue(limitedCache.contains("third"))
    }

    @Test
    fun maxEntries_reducesToMaxAfterEviction() = runTest {
        database.close()
        database = Room.inMemoryDatabaseBuilder<HalogenDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        val limitedCache = RoomThemeCache(
            dao = database.themeDao(),
            config = RoomThemeCacheConfig(maxEntries = 2),
        )

        limitedCache.put("first", oceanSpec)
        limitedCache.put("second", neonSpec)
        limitedCache.put("third", oceanSpec)

        assertEquals(2, limitedCache.size(), "Size should equal maxEntries after eviction")
        assertTrue(limitedCache.contains("third"), "Newest entry should always survive")
    }

    // ── observeChanges ──────────────────────────────────────────────────

    @Test
    fun observeChanges_emitsInserted_onPut() = runTest {
        val received = CompletableDeferred<CacheEvent>()
        launch(start = CoroutineStart.UNDISPATCHED) {
            received.complete(cache.observeChanges().first())
        }
        cache.put("ocean", oceanSpec, ThemeSource.LLM)
        val event = received.await()
        assertIs<CacheEvent.Inserted>(event)
        assertEquals("ocean", event.key)
        assertEquals(ThemeSource.LLM, event.source)
    }

    @Test
    fun observeChanges_emitsEvicted_onEvict() = runTest {
        val received = CompletableDeferred<CacheEvent>()
        launch(start = CoroutineStart.UNDISPATCHED) {
            received.complete(cache.observeChanges().first { it is CacheEvent.Evicted })
        }
        cache.put("ocean", oceanSpec)
        cache.evict("ocean")
        val event = received.await()
        assertIs<CacheEvent.Evicted>(event)
        assertEquals("ocean", event.key)
    }

    @Test
    fun observeChanges_emitsEvictedBatch_onBatchEvict() = runTest {
        val received = CompletableDeferred<CacheEvent>()
        launch(start = CoroutineStart.UNDISPATCHED) {
            received.complete(cache.observeChanges().first { it is CacheEvent.EvictedBatch })
        }
        cache.put("ocean", oceanSpec)
        cache.put("neon", neonSpec)
        cache.evict(setOf("ocean", "neon"))
        val event = received.await()
        assertIs<CacheEvent.EvictedBatch>(event)
        assertEquals(setOf("ocean", "neon"), event.keys)
    }

    @Test
    fun observeChanges_emitsCleared_onClear() = runTest {
        val received = CompletableDeferred<CacheEvent>()
        launch(start = CoroutineStart.UNDISPATCHED) {
            received.complete(cache.observeChanges().first { it is CacheEvent.Cleared })
        }
        cache.put("ocean", oceanSpec)
        cache.clear()
        val event = received.await()
        assertIs<CacheEvent.Cleared>(event)
    }

    // ── Test data ───────────────────────────────────────────────────────

    private companion object {
        const val OCEAN_SPEC_JSON =
            """{"pri":"#356A8A","sec":"#5C8A9E","ter":"#7AACB5","neuL":"#F0F5F7","neuD":"#0E1B26","err":"#BA1A1A","font":"modern","hw":500,"bw":400,"ls":false,"cs":"soft","cx":1.2}"""
        const val NEON_SPEC_JSON =
            """{"pri":"#9A6ACD","sec":"#4A8A8A","ter":"#B06B7D","neuL":"#F3F0F6","neuD":"#151018","err":"#93000A","font":"mono","hw":700,"bw":400,"ls":true,"cs":"sharp","cx":0.5}"""
    }
}
