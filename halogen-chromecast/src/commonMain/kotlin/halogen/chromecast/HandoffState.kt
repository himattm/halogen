package halogen.chromecast

/**
 * Observable state of an in-flight Halogen cast handoff.
 *
 * Typical transitions:
 * * `Idle` → `Sending` → `Acknowledged` (happy path)
 * * `Sending` → `Failed` (ack timeout or transport error)
 * * any state → `Disconnected` when the Cast session drops
 */
public sealed class HandoffState {

    /** No outstanding send; the session is ready to ship the next theme. */
    public data object Idle : HandoffState()

    /** A Cast session is coming up. [deviceName] may be null until the SDK reports it. */
    public data class Connecting(val deviceName: String?) : HandoffState()

    /** A theme message is in flight; waiting for [Ack]. */
    public data class Sending(val key: String, val label: String?) : HandoffState()

    /** Receiver confirmed the last theme. Phone UI can render the "Now playing on X" moment. */
    public data class Acknowledged(
        val deviceName: String,
        val key: String,
        val label: String?,
        val at: Long,
    ) : HandoffState()

    /** The last send failed. [reason] is a short human-readable description. */
    public data class Failed(val reason: String, val cause: Throwable? = null) : HandoffState()

    /** No Cast session is active. */
    public data object Disconnected : HandoffState()
}

/**
 * Outcome of a single [HalogenCastSession.sendTheme] or
 * [HalogenCastSession.sendPartial] call.
 */
public sealed interface HandoffResult {

    /** Receiver acknowledged the theme. */
    public data class Acknowledged(val deviceName: String) : HandoffResult

    /** Receiver rejected the theme, or the transport reported an error. */
    public data class Failed(val reason: String) : HandoffResult

    /** No ack arrived within the configured timeout. */
    public data object TimedOut : HandoffResult
}
