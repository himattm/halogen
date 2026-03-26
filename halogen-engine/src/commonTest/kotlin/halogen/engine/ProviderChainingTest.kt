package halogen.engine

import halogen.HalogenLlmAvailability
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ProviderChainingTest {

    @Test
    fun resolve_primaryFails_fallbackSucceeds() = runTest {
        val primary = FakeLlmProvider(shouldFail = true)
        val fallback = FakeLlmProvider(responseJson = TestFixtures.NEON_SPEC_JSON)
        val engine = Halogen.Builder()
            .provider(primary)
            .fallbackProvider(fallback)
            .scope(TestScope())
            .tokenBudget(Int.MAX_VALUE)
            .noPersistence()
            .build()

        val result = engine.resolve("test", "test")
        assertIs<HalogenResult.Success>(result)
        assertEquals(TestFixtures.NEON_SPEC, result.spec)
        assertEquals(1, primary.generateCallCount)
        assertEquals(1, fallback.generateCallCount)
    }

    @Test
    fun resolve_primaryUnavailable_fallbackReady() = runTest {
        val primary = FakeLlmProvider(availability = HalogenLlmAvailability.UNAVAILABLE)
        val fallback = FakeLlmProvider(responseJson = TestFixtures.NEON_SPEC_JSON)
        val engine = Halogen.Builder()
            .provider(primary)
            .fallbackProvider(fallback)
            .scope(TestScope())
            .tokenBudget(Int.MAX_VALUE)
            .noPersistence()
            .build()

        val result = engine.resolve("test", "test")
        assertIs<HalogenResult.Success>(result)
        assertEquals(0, primary.generateCallCount)
        assertEquals(1, fallback.generateCallCount)
    }

    @Test
    fun resolve_allProvidersFail_returnsUnavailable() = runTest {
        val primary = FakeLlmProvider(shouldFail = true)
        val fallback = FakeLlmProvider(shouldFail = true)
        val engine = Halogen.Builder()
            .provider(primary)
            .fallbackProvider(fallback)
            .scope(TestScope())
            .tokenBudget(Int.MAX_VALUE)
            .noPersistence()
            .build()

        val result = engine.resolve("test", "test")
        assertIs<HalogenResult.Unavailable>(result)
    }

    @Test
    fun resolve_firstProviderReady_secondNeverCalled() = runTest {
        val primary = FakeLlmProvider()
        val fallback = FakeLlmProvider()
        val engine = Halogen.Builder()
            .provider(primary)
            .fallbackProvider(fallback)
            .scope(TestScope())
            .tokenBudget(Int.MAX_VALUE)
            .noPersistence()
            .build()

        engine.resolve("test", "test")
        assertEquals(1, primary.generateCallCount)
        assertEquals(0, fallback.generateCallCount)
    }
}
