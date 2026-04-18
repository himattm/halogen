package halogen.chromecast

import halogen.HalogenThemeSpec
import halogen.engine.HalogenEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Default ack timeout used by [handoffToChromecast] when none is supplied. */
public const val DEFAULT_CAST_ACK_TIMEOUT_MS: Long = 2_500L

/**
 * Attach a [HalogenCastSession] to this [HalogenEngine] and, when [autoSend] is
 * true, forward every [HalogenEngine.activeTheme] emission to the receiver.
 *
 * Deduplication: consecutive emissions that match the last sent `(key, spec)`
 * are suppressed so rapid, idempotent re-applies of the same theme don't flood
 * the wire.
 *
 * The returned [HalogenCastSession] is owned by [scope]; cancelling [scope]
 * (or calling [HalogenCastSession.close]) stops forwarding.
 *
 * @param transport    Consumer-provided Cast transport.
 * @param scope        Scope owning the forwarding coroutine.
 * @param senderInfo   Metadata attached to outgoing theme payloads.
 * @param autoSend     When true, auto-forwards [HalogenEngine.activeTheme] changes.
 * @param ackTimeoutMs Milliseconds to wait for an [Ack] before reporting `TimedOut`.
 */
public fun HalogenEngine.handoffToChromecast(
    transport: CastMessageTransport,
    scope: CoroutineScope,
    senderInfo: SenderInfo,
    autoSend: Boolean = true,
    ackTimeoutMs: Long = DEFAULT_CAST_ACK_TIMEOUT_MS,
): HalogenCastSession {
    val session = HalogenCastSession(
        transport = transport,
        scope = scope,
        senderInfo = senderInfo,
        ackTimeoutMs = ackTimeoutMs,
        now = ::halogenCastNow,
    )

    if (autoSend) {
        scope.launch {
            var last: Pair<String, HalogenThemeSpec>? = null
            activeTheme.collectLatest { spec ->
                val key = activeKey.value ?: return@collectLatest
                if (spec == null) return@collectLatest
                val snapshot = key to spec
                if (snapshot == last) return@collectLatest
                last = snapshot
                session.sendTheme(key = key, spec = spec)
            }
        }
    }

    return session
}
