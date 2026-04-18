package halogen.chromecast

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Plug-point for the host platform's Cast SDK.
 *
 * This library is deliberately Cast-SDK-free; consumers adapt their Cast session
 * (e.g. Android `CastSession`, iOS `GCKCastSession`) to this interface so the
 * orchestration and wire-format code here stays multiplatform.
 *
 * Implementations must be safe to call from a coroutine context.
 */
public interface CastMessageTransport {

    /** Cast namespace this transport is bound to. Must start with `urn:x-cast:`. */
    public val namespace: String

    /**
     * Send an encoded [payload] to the receiver. Suspends until the Cast SDK
     * has accepted the message for delivery (this is **not** the receiver ack —
     * the ack arrives asynchronously via [incoming]).
     */
    public suspend fun send(payload: String)

    /** Hot flow of raw JSON payloads received from the receiver on [namespace]. */
    public val incoming: Flow<String>

    /** `true` whenever a Cast session is active. Used to drive the Disconnected state. */
    public val isConnected: StateFlow<Boolean>

    /** Friendly name of the connected device, or `null` when unavailable. */
    public val deviceName: StateFlow<String?>
}
