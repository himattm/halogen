package halogen

/**
 * Interface for generating theme JSON from a natural language hint.
 * Implement this to connect any LLM -- on-device or cloud.
 */
public interface HalogenLlmProvider {

    /**
     * Generate a [HalogenThemeSpec] JSON string from a prompt.
     *
     * @param prompt The fully constructed prompt (system + few-shot + user hint).
     *               The engine builds this; the provider just sends it.
     * @return Raw JSON string response from the LLM.
     * @throws HalogenLlmException on failure.
     */
    public suspend fun generate(prompt: String): String

    /**
     * Check if this provider is currently available.
     * For Gemini Nano: checks AICore status.
     * For cloud providers: could check network or API key presence.
     */
    public suspend fun availability(): HalogenLlmAvailability
}

/**
 * Availability status for an [HalogenLlmProvider].
 */
public enum class HalogenLlmAvailability {
    /** Can generate right now. */
    READY,
    /** Downloading model, warming up, etc. */
    INITIALIZING,
    /** Not supported on this device / no API key / no network. */
    UNAVAILABLE,
}

/**
 * Exception thrown by [HalogenLlmProvider] implementations on failure.
 */
public class HalogenLlmException(
    message: String,
    cause: Throwable? = null,
    public val isRetryable: Boolean = false,
) : Exception(message, cause)
