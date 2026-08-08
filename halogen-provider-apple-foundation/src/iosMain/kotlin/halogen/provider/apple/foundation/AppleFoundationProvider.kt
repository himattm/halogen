package halogen.provider.apple.foundation

import halogen.HalogenLlmAvailability
import halogen.HalogenLlmException
import halogen.HalogenLlmProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Apple Foundation Models on-device AI provider for Halogen.
 *
 * Wraps a Swift-implemented [AppleFoundationBridge] (which calls
 * `LanguageModelSession.respond(to:)` / `SystemLanguageModel.default.availability`
 * from `import FoundationModels`) and adapts its callback-style API to the
 * [HalogenLlmProvider] suspend interface.
 *
 * **Device requirements**: iOS 26+, an Apple-Intelligence-eligible device, and
 * Apple Intelligence enabled in Settings. On any unsupported configuration
 * [availability] returns [HalogenLlmAvailability.UNAVAILABLE] and the caller
 * should fall back to a different provider.
 */
public class AppleFoundationProvider(
    private val bridge: AppleFoundationBridge,
) : HalogenLlmProvider {

    override suspend fun generate(prompt: String): String =
        suspendCancellableCoroutine { cont ->
            bridge.generate(prompt) { result, error ->
                when {
                    error != null -> cont.resumeWithException(
                        HalogenLlmException(
                            message = "Apple Foundation Models generation failed: ${error.localizedDescription}",
                            isRetryable = true,
                        ),
                    )
                    result != null -> cont.resume(result)
                    else -> cont.resumeWithException(
                        HalogenLlmException(
                            "Empty response from Apple Foundation Models",
                            isRetryable = false,
                        ),
                    )
                }
            }
        }

    override suspend fun availability(): HalogenLlmAvailability =
        suspendCancellableCoroutine { cont ->
            bridge.availability { status, _ ->
                val mapped = when (status) {
                    "available" -> HalogenLlmAvailability.READY
                    "initializing" -> HalogenLlmAvailability.INITIALIZING
                    else -> HalogenLlmAvailability.UNAVAILABLE
                }
                cont.resume(mapped)
            }
        }
}
