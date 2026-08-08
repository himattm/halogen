package halogen.provider.apple.foundation

import platform.Foundation.NSError

/**
 * Native-side bridge for Apple's Foundation Models framework (iOS 26+).
 *
 * The framework is Swift-only, so the actual `LanguageModelSession` /
 * `SystemLanguageModel` calls must live in a Swift class that implements
 * this protocol. [AppleFoundationProvider] adapts the callback-based
 * methods exposed here into the suspend-based [halogen.HalogenLlmProvider]
 * contract.
 *
 * Implementations are expected to dispatch work onto an appropriate queue
 * (`Task { ... }` is fine) and to invoke the completion handler exactly
 * once on either success or failure.
 */
public interface AppleFoundationBridge {

    /**
     * Generate raw JSON theme text for [prompt].
     *
     * The completion handler must be invoked with exactly one of:
     * - a non-null result string and `null` error on success
     * - `null` result and a non-null [NSError] on failure
     */
    public fun generate(prompt: String, completion: (String?, NSError?) -> Unit)

    /**
     * Probe model availability.
     *
     * The completion handler receives a status string and an optional reason:
     * - `"available"` — model ready for inference
     * - `"initializing"` — model is downloading or warming up
     * - `"unavailable"` — device not eligible, Apple Intelligence disabled,
     *   running below iOS 26, or any other unsupported state
     *
     * The reason (e.g. `"deviceNotEligible"`, `"appleIntelligenceNotEnabled"`,
     * `"modelNotReady"`, `"ios<26"`) is informational and may be `null`.
     */
    public fun availability(completion: (String, String?) -> Unit)
}
