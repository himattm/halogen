package halogen.engine

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MemoryThemeCacheTest {

    @Test
    fun get_returnsNull_whenEmpty() = runTest {
        val cache = MemoryThemeCache()
        assertNull(cache.get("nonexistent"))
    }

    @Test
    fun put_then_get_returnsSpec() = runTest {
        val cache = MemoryThemeCache()
        cache.put("ocean", TestFixtures.OCEAN_SPEC)
        assertEquals(TestFixtures.OCEAN_SPEC, cache.get("ocean"))
    }

    @Test
    fun put_then_contains_returnsTrue() = runTest {
        val cache = MemoryThemeCache()
        assertFalse(cache.contains("ocean"))
        cache.put("ocean", TestFixtures.OCEAN_SPEC)
        assertTrue(cache.contains("ocean"))
    }

    @Test
    fun evict_removesEntry() = runTest {
        val cache = MemoryThemeCache()
        cache.put("ocean", TestFixtures.OCEAN_SPEC)
        cache.evict("ocean")
        assertNull(cache.get("ocean"))
    }

    @Test
    fun evictBatch_removesAllSpecifiedKeys() = runTest {
        val cache = MemoryThemeCache()
        cache.put("ocean", TestFixtures.OCEAN_SPEC)
        cache.put("neon", TestFixtures.NEON_SPEC)
        cache.evict(setOf("ocean", "neon"))
        assertEquals(0, cache.size())
    }

    @Test
    fun clear_removesEverything() = runTest {
        val cache = MemoryThemeCache()
        cache.put("ocean", TestFixtures.OCEAN_SPEC)
        cache.put("neon", TestFixtures.NEON_SPEC)
        cache.clear()
        assertEquals(0, cache.size())
    }

    @Test
    fun keys_returnsAllStoredKeys() = runTest {
        val cache = MemoryThemeCache()
        cache.put("ocean", TestFixtures.OCEAN_SPEC)
        cache.put("neon", TestFixtures.NEON_SPEC)
        assertEquals(setOf("ocean", "neon"), cache.keys())
    }

    @Test
    fun size_returnsCorrectCount() = runTest {
        val cache = MemoryThemeCache()
        assertEquals(0, cache.size())
        cache.put("ocean", TestFixtures.OCEAN_SPEC)
        assertEquals(1, cache.size())
        cache.put("neon", TestFixtures.NEON_SPEC)
        assertEquals(2, cache.size())
    }

    @Test
    fun entries_returnsMetadataForAllEntries() = runTest {
        val cache = MemoryThemeCache()
        cache.put("ocean", TestFixtures.OCEAN_SPEC, ThemeSource.LLM)
        cache.put("neon", TestFixtures.NEON_SPEC, ThemeSource.SERVER)
        val entries = cache.entries()
        assertEquals(2, entries.size)
        val keys = entries.map { it.key }.toSet()
        assertEquals(setOf("ocean", "neon"), keys)
        val sources = entries.associate { it.key to it.source }
        assertEquals(ThemeSource.LLM, sources["ocean"])
        assertEquals(ThemeSource.SERVER, sources["neon"])
    }

    @Test
    fun lruEviction_oldestRemovedWhenOverCapacity() = runTest {
        val cache = MemoryThemeCache(maxEntries = 2)
        cache.put("first", TestFixtures.OCEAN_SPEC)
        cache.put("second", TestFixtures.NEON_SPEC)
        cache.put("third", TestFixtures.OCEAN_SPEC) // should evict "first"
        assertNull(cache.get("first"))
        assertEquals(2, cache.size())
        assertTrue(cache.contains("second"))
        assertTrue(cache.contains("third"))
    }

    @Test
    fun lruEviction_overCapacity_reducesToMax() = runTest {
        val cache = MemoryThemeCache(maxEntries = 2)
        cache.put("first", TestFixtures.OCEAN_SPEC)
        cache.put("second", TestFixtures.NEON_SPEC)
        cache.put("third", TestFixtures.OCEAN_SPEC)
        // Should have evicted one entry, size back to maxEntries
        assertEquals(2, cache.size())
        assertTrue(cache.contains("third"), "Newest entry should always survive")
    }

    @Test
    fun observeChanges_emitsInserted() = runTest {
        val cache = MemoryThemeCache()
        val received = CompletableDeferred<CacheEvent>()
        launch(start = CoroutineStart.UNDISPATCHED) {
            received.complete(cache.observeChanges().first())
        }
        cache.put("ocean", TestFixtures.OCEAN_SPEC, ThemeSource.LLM)
        val event = received.await()
        assertIs<CacheEvent.Inserted>(event)
        assertEquals("ocean", event.key)
    }

    @Test
    fun observeChanges_emitsEvicted() = runTest {
        val cache = MemoryThemeCache()
        val received = CompletableDeferred<CacheEvent>()
        launch(start = CoroutineStart.UNDISPATCHED) {
            received.complete(cache.observeChanges().first { it is CacheEvent.Evicted })
        }
        cache.put("ocean", TestFixtures.OCEAN_SPEC)
        cache.evict("ocean")
        val event = received.await()
        assertIs<CacheEvent.Evicted>(event)
        assertEquals("ocean", event.key)
    }

    @Test
    fun observeChanges_emitsCleared() = runTest {
        val cache = MemoryThemeCache()
        val received = CompletableDeferred<CacheEvent>()
        launch(start = CoroutineStart.UNDISPATCHED) {
            received.complete(cache.observeChanges().first { it is CacheEvent.Cleared })
        }
        cache.put("ocean", TestFixtures.OCEAN_SPEC)
        cache.clear()
        val event = received.await()
        assertIs<CacheEvent.Cleared>(event)
    }
}
