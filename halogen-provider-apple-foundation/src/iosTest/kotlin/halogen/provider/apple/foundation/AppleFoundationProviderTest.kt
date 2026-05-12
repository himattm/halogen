package halogen.provider.apple.foundation

import halogen.HalogenLlmAvailability
import halogen.HalogenLlmException
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private class FakeBridge(
    private val generateResult: String? = null,
    private val generateError: NSError? = null,
    private val availabilityStatus: String = "available",
    private val availabilityReason: String? = null,
) : AppleFoundationBridge {
    override fun generate(prompt: String, completion: (String?, NSError?) -> Unit) {
        completion(generateResult, generateError)
    }

    override fun availability(completion: (String, String?) -> Unit) {
        completion(availabilityStatus, availabilityReason)
    }
}

class AppleFoundationProviderTest {

    @Test
    fun generate_returnsResultFromBridge() = runTest {
        val provider = AppleFoundationProvider(FakeBridge(generateResult = "{\"pri\":\"#abc\"}"))
        assertEquals("{\"pri\":\"#abc\"}", provider.generate("warm coffee shop"))
    }

    @Test
    fun generate_wrapsNSErrorAsRetryableException() = runTest {
        val error = NSError.errorWithDomain("halogen.test", 42, null)
        val provider = AppleFoundationProvider(FakeBridge(generateError = error))
        val ex = assertFailsWith<HalogenLlmException> { provider.generate("hint") }
        assertTrue(ex.isRetryable, "NSError-derived failures should be retryable")
    }

    @Test
    fun generate_emptyResponseThrowsNonRetryable() = runTest {
        val provider = AppleFoundationProvider(FakeBridge(generateResult = null, generateError = null))
        val ex = assertFailsWith<HalogenLlmException> { provider.generate("hint") }
        assertEquals(false, ex.isRetryable)
    }

    @Test
    fun availability_mapsAvailableToReady() = runTest {
        val provider = AppleFoundationProvider(FakeBridge(availabilityStatus = "available"))
        assertEquals(HalogenLlmAvailability.READY, provider.availability())
    }

    @Test
    fun availability_mapsInitializingToInitializing() = runTest {
        val provider = AppleFoundationProvider(
            FakeBridge(availabilityStatus = "initializing", availabilityReason = "modelNotReady"),
        )
        assertEquals(HalogenLlmAvailability.INITIALIZING, provider.availability())
    }

    @Test
    fun availability_mapsUnavailableToUnavailable() = runTest {
        val provider = AppleFoundationProvider(
            FakeBridge(availabilityStatus = "unavailable", availabilityReason = "ios<26"),
        )
        assertEquals(HalogenLlmAvailability.UNAVAILABLE, provider.availability())
    }

    @Test
    fun availability_mapsUnknownStatusToUnavailable() = runTest {
        val provider = AppleFoundationProvider(FakeBridge(availabilityStatus = "garbage"))
        assertEquals(HalogenLlmAvailability.UNAVAILABLE, provider.availability())
    }
}
