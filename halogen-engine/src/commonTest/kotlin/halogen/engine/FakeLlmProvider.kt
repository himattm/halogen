package halogen.engine

import halogen.HalogenLlmAvailability
import halogen.HalogenLlmException
import halogen.HalogenLlmProvider

internal class FakeLlmProvider(
    private var availability: HalogenLlmAvailability = HalogenLlmAvailability.READY,
    private var responseJson: String = TestFixtures.OCEAN_SPEC_JSON,
    private var shouldFail: Boolean = false,
) : HalogenLlmProvider {

    var generateCallCount = 0; private set
    var lastPrompt: String? = null; private set

    override suspend fun generate(prompt: String): String {
        generateCallCount++
        lastPrompt = prompt
        if (shouldFail) throw HalogenLlmException("Fake provider failure")
        return responseJson
    }

    override suspend fun availability(): HalogenLlmAvailability = availability

    fun setAvailability(a: HalogenLlmAvailability) { availability = a }
    fun setResponse(json: String) { responseJson = json }
    fun setShouldFail(fail: Boolean) { shouldFail = fail }
}
