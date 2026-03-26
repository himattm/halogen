package halogen.engine

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class CustomPromptEngineTest {

    @Test
    fun promptInstructions_appearsInPromptPassedToProvider() = runTest {
        val provider = FakeLlmProvider()
        val engine = Halogen.Builder()
            .provider(provider)
            .promptInstructions("Always use brand blue #1A73E8")
            .scope(TestScope())
            .tokenBudget(Int.MAX_VALUE)
            .noPersistence()
            .build()

        engine.resolve("test", "test theme")
        assertContains(provider.lastPrompt!!, "Always use brand blue #1A73E8")
    }

    @Test
    fun promptExamples_appearInPromptPassedToProvider() = runTest {
        val provider = FakeLlmProvider()
        val engine = Halogen.Builder()
            .provider(provider)
            .promptExamples("quarterly earnings" to """{"pri":"#1A73E8"}""")
            .scope(TestScope())
            .tokenBudget(Int.MAX_VALUE)
            .noPersistence()
            .build()

        engine.resolve("test", "test theme")
        assertContains(provider.lastPrompt!!, "quarterly earnings")
        assertContains(provider.lastPrompt!!, "#1A73E8")
    }

    @Test
    fun tokenBudget_exceededAtBuild_throwsWithBreakdown() {
        val hugeInstructions = "x".repeat(20000)
        val exception = assertFailsWith<IllegalStateException> {
            Halogen.Builder()
                .provider(FakeLlmProvider())
                .promptInstructions(hugeInstructions)
                .tokenBudget(100)
                .noPersistence()
                .build()
        }
        assertContains(exception.message!!, "token budget")
    }

    @Test
    fun tokenBudget_withinLimit_buildsSuccessfully() {
        val engine = Halogen.Builder()
            .provider(FakeLlmProvider())
            .promptInstructions("Short instruction")
            .tokenBudget(4000)
            .noPersistence()
            .build()
        assertIs<HalogenEngine>(engine)
    }

    @Test
    fun tokenBudget_maxValue_disablesValidation() {
        val hugeInstructions = "x".repeat(20000)
        val engine = Halogen.Builder()
            .provider(FakeLlmProvider())
            .promptInstructions(hugeInstructions)
            .tokenBudget(Int.MAX_VALUE)
            .noPersistence()
            .build()
        assertIs<HalogenEngine>(engine)
    }
}
