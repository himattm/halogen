package halogen.engine

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThemeExportTest {

    private fun buildEngine(cache: ThemeCache = MemoryThemeCache()): HalogenEngine {
        return Halogen.Builder()
            .provider(FakeLlmProvider())
            .cache(cache)
            .scope(TestScope())
            .tokenBudget(Int.MAX_VALUE)
            .noPersistence()
            .build()
    }

    @Test
    fun exportThemes_emptyCache_returnsEmptyBundle() = runTest {
        val engine = buildEngine()
        val json = engine.exportThemes()
        assertTrue(json.contains("\"themes\":{}"), "Should contain empty themes map")
    }

    @Test
    fun exportThemes_withEntries_includesAllThemes() = runTest {
        val cache = MemoryThemeCache()
        cache.put("ocean", TestFixtures.OCEAN_SPEC)
        cache.put("neon", TestFixtures.NEON_SPEC)
        val engine = buildEngine(cache)
        val json = engine.exportThemes()
        assertTrue(json.contains("ocean"), "Should contain ocean key")
        assertTrue(json.contains("neon"), "Should contain neon key")
        assertTrue(json.contains("#356A8A"), "Should contain ocean primary color")
    }

    @Test
    fun importThemes_validBundle_populatesCache() = runTest {
        val sourceCache = MemoryThemeCache()
        sourceCache.put("ocean", TestFixtures.OCEAN_SPEC)
        sourceCache.put("neon", TestFixtures.NEON_SPEC)
        val sourceEngine = buildEngine(sourceCache)
        val exported = sourceEngine.exportThemes()

        val targetCache = MemoryThemeCache()
        val targetEngine = buildEngine(targetCache)
        val count = targetEngine.importThemes(exported)
        assertEquals(2, count)
        assertEquals(TestFixtures.OCEAN_SPEC, targetCache.get("ocean"))
        assertEquals(TestFixtures.NEON_SPEC, targetCache.get("neon"))
    }

    @Test
    fun exportTheme_existingKey_returnsBundle() = runTest {
        val cache = MemoryThemeCache()
        cache.put("ocean", TestFixtures.OCEAN_SPEC)
        val engine = buildEngine(cache)
        val json = engine.exportTheme("ocean")
        assertTrue(json != null, "Should return non-null for existing key")
        assertTrue(json!!.contains("#356A8A"), "Should contain ocean primary color")
    }

    @Test
    fun exportTheme_missingKey_returnsNull() = runTest {
        val engine = buildEngine()
        assertNull(engine.exportTheme("nonexistent"))
    }

    @Test
    fun importTheme_addsToCache() = runTest {
        val cache = MemoryThemeCache()
        cache.put("ocean", TestFixtures.OCEAN_SPEC)
        val engine = buildEngine(cache)
        val exported = engine.exportTheme("ocean")!!

        val targetCache = MemoryThemeCache()
        val targetEngine = buildEngine(targetCache)
        targetEngine.importTheme("my-ocean", exported)
        assertEquals(TestFixtures.OCEAN_SPEC, targetCache.get("my-ocean"))
    }

    @Test
    fun roundTrip_exportThenImport_preservesSpecs() = runTest {
        val cache = MemoryThemeCache()
        cache.put("ocean", TestFixtures.OCEAN_SPEC)
        cache.put("neon", TestFixtures.NEON_SPEC)
        val engine = buildEngine(cache)

        val exported = engine.exportThemes()

        val targetCache = MemoryThemeCache()
        val targetEngine = buildEngine(targetCache)
        targetEngine.importThemes(exported)

        assertEquals(TestFixtures.OCEAN_SPEC, targetCache.get("ocean"))
        assertEquals(TestFixtures.NEON_SPEC, targetCache.get("neon"))
    }
}
