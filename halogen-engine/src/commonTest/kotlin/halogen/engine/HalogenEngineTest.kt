package halogen.engine

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HalogenEngineTest {

    private fun buildEngine(
        provider: FakeLlmProvider = FakeLlmProvider(),
        cache: ThemeCache = MemoryThemeCache(),
    ): HalogenEngine {
        val scope = TestScope()
        return Halogen.Builder()
            .provider(provider)
            .cache(cache)
            .scope(scope)
            .tokenBudget(Int.MAX_VALUE)
            .noPersistence()
            .build()
    }

    @Test
    fun resolve_cacheMiss_callsLlm_returnsSuccess() = runTest {
        val provider = FakeLlmProvider()
        val engine = buildEngine(provider)
        val result = engine.resolve("ocean", "ocean vibes")
        assertIs<HalogenResult.Success>(result)
        assertEquals(1, provider.generateCallCount)
    }

    @Test
    fun resolve_cacheHit_doesNotCallLlm() = runTest {
        val provider = FakeLlmProvider()
        val engine = buildEngine(provider)
        engine.resolve("ocean", "ocean vibes")
        assertEquals(1, provider.generateCallCount)

        val result = engine.resolve("ocean")
        assertIs<HalogenResult.Cached>(result)
        assertEquals(1, provider.generateCallCount)
    }

    @Test
    fun resolve_llmUnavailable_returnsUnavailable() = runTest {
        val provider = FakeLlmProvider(availability = halogen.HalogenLlmAvailability.UNAVAILABLE)
        val engine = buildEngine(provider)
        val result = engine.resolve("ocean", "ocean vibes")
        assertIs<HalogenResult.Unavailable>(result)
    }

    @Test
    fun resolve_llmFails_returnsUnavailable() = runTest {
        val provider = FakeLlmProvider(shouldFail = true)
        val engine = buildEngine(provider)
        val result = engine.resolve("ocean", "ocean vibes")
        assertIs<HalogenResult.Unavailable>(result)
    }

    @Test
    fun resolve_setsActiveTheme() = runTest {
        val engine = buildEngine()
        assertNull(engine.activeTheme.value)
        engine.resolve("ocean", "ocean vibes")
        assertEquals(TestFixtures.OCEAN_SPEC, engine.activeTheme.value)
    }

    @Test
    fun resolve_setsActiveKey() = runTest {
        val engine = buildEngine()
        assertNull(engine.activeKey.value)
        engine.resolve("ocean", "ocean vibes")
        assertEquals("ocean", engine.activeKey.value)
    }

    @Test
    fun resolve_cachingDisabled_alwaysCallsLlm() = runTest {
        val provider = FakeLlmProvider()
        val engine = buildEngine(provider)
        engine.cachingEnabled = false
        engine.resolve("ocean", "ocean vibes")
        engine.resolve("ocean", "ocean vibes")
        assertEquals(2, provider.generateCallCount)
    }

    @Test
    fun prefetch_doesNotSetActiveTheme() = runTest {
        val engine = buildEngine()
        assertNull(engine.activeTheme.value)
        engine.prefetch("ocean", "ocean vibes")
        assertNull(engine.activeTheme.value)
    }

    @Test
    fun regenerate_bypassesCache() = runTest {
        val provider = FakeLlmProvider()
        val engine = buildEngine(provider)
        engine.resolve("ocean", "ocean vibes")
        assertEquals(1, provider.generateCallCount)

        provider.setResponse(TestFixtures.NEON_SPEC_JSON)
        val result = engine.regenerate("ocean", "neon cyberpunk")
        assertIs<HalogenResult.Success>(result)
        assertEquals(2, provider.generateCallCount)
        assertEquals(TestFixtures.NEON_SPEC, engine.activeTheme.value)
    }

    @Test
    fun refresh_evictsAndReResolves() = runTest {
        val provider = FakeLlmProvider()
        val cache = MemoryThemeCache()
        val engine = buildEngine(provider, cache)
        engine.resolve("ocean", "ocean vibes")
        assertTrue(cache.contains("ocean"))

        provider.setResponse(TestFixtures.NEON_SPEC_JSON)
        val result = engine.refresh("ocean", "ocean vibes")
        assertIs<HalogenResult.Success>(result)
        assertEquals(2, provider.generateCallCount)
    }

    @Test
    fun evict_removesFromCache() = runTest {
        val cache = MemoryThemeCache()
        val engine = buildEngine(cache = cache)
        engine.resolve("ocean", "ocean vibes")
        assertTrue(cache.contains("ocean"))
        engine.evict("ocean")
        assertTrue(!cache.contains("ocean"))
    }

    @Test
    fun clearCache_removesAll() = runTest {
        val cache = MemoryThemeCache()
        val engine = buildEngine(cache = cache)
        engine.resolve("ocean", "ocean vibes")
        assertTrue(cache.size() > 0)
        engine.clearCache()
        assertEquals(0, cache.size())
    }
}
