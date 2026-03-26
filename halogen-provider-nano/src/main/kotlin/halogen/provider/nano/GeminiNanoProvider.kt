package halogen.provider.nano

import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.GenerateContentResponse
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import halogen.HalogenLlmAvailability
import halogen.HalogenLlmException
import halogen.HalogenLlmProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Gemini Nano on-device AI provider for Halogen.
 *
 * Uses the ML Kit Generative AI Prompt API (`com.google.mlkit:genai-prompt`)
 * — no API key or network required for inference.
 *
 * **Device requirements**: Pixel 9+, Samsung S24+, or other devices with
 * on-device Gemini Nano support and a locked bootloader.
 *
 * @param temperature Controls randomness in token selection (0.0–1.0). Default 0.2.
 * @param topK Controls diversity of results. Default 10.
 */
public class GeminiNanoProvider(
    private val temperature: Float = 0.2f,
    private val topK: Int = 10,
) : HalogenLlmProvider {

    private val generativeModel: GenerativeModel by lazy {
        Generation.getClient()
    }

    override suspend fun generate(prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val request: GenerateContentRequest = generateContentRequest(
                    TextPart(prompt),
                ) {
                    this.temperature = this@GeminiNanoProvider.temperature
                    this.topK = this@GeminiNanoProvider.topK
                }
                val response: GenerateContentResponse =
                    generativeModel.generateContent(request)
                response.candidates.firstOrNull()?.text
                    ?: throw HalogenLlmException(
                        "Empty response from Gemini Nano — no candidates returned",
                        isRetryable = false,
                    )
            } catch (e: HalogenLlmException) {
                throw e
            } catch (e: Exception) {
                throw HalogenLlmException(
                    "Gemini Nano generation failed: ${e.message}",
                    cause = e,
                    isRetryable = true,
                )
            }
        }
    }

    override suspend fun availability(): HalogenLlmAvailability {
        return withContext(Dispatchers.IO) {
            try {
                when (generativeModel.checkStatus()) {
                    FeatureStatus.AVAILABLE -> HalogenLlmAvailability.READY
                    FeatureStatus.DOWNLOADABLE,
                    FeatureStatus.DOWNLOADING,
                    -> HalogenLlmAvailability.INITIALIZING
                    else -> HalogenLlmAvailability.UNAVAILABLE
                }
            } catch (_: Exception) {
                HalogenLlmAvailability.UNAVAILABLE
            }
        }
    }

    /**
     * Trigger the on-device model download if status is [FeatureStatus.DOWNLOADABLE].
     *
     * Collect the returned [Flow] to observe download progress:
     * ```kotlin
     * provider.downloadModel().collect { status ->
     *     when (status) {
     *         is DownloadStatus.DownloadStarted -> { /* … */ }
     *         is DownloadStatus.DownloadProgress -> { /* bytes downloaded */ }
     *         DownloadStatus.DownloadCompleted -> { /* ready to generate */ }
     *         is DownloadStatus.DownloadFailed -> { /* handle error */ }
     *     }
     * }
     * ```
     */
    public fun downloadModel(): Flow<DownloadStatus> = generativeModel.download()

    /**
     * Pre-load the model into memory to reduce first-inference latency.
     * Call this early (e.g. in `onCreate`) so the model is warm when the
     * user first requests a theme.
     */
    public suspend fun warmup() {
        withContext(Dispatchers.IO) {
            generativeModel.warmup()
        }
    }

    /**
     * Release resources held by the underlying [GenerativeModel].
     * Call when this provider is no longer needed.
     */
    public fun close() {
        generativeModel.close()
    }
}
