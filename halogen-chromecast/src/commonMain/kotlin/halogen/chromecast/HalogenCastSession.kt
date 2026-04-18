package halogen.chromecast

import halogen.HalogenThemeSpec
import halogen.chromecast.internal.generateMessageId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

/**
 * Orchestrates a single Halogen cast handoff stream over a [CastMessageTransport].
 *
 * Use [HalogenCastSession.sendTheme] to ship a new palette to the receiver, or
 * have the engine integration (`HalogenEngine.handoffToChromecast`) auto-send
 * every `activeTheme` emission. Observe [state] to drive the phone-side
 * "Now playing on [device]" confirmation.
 *
 * Call [close] when finished (e.g. ViewModel.onCleared) to cancel the internal
 * coroutines that watch the transport for acks.
 */
public class HalogenCastSession internal constructor(
    private val transport: CastMessageTransport,
    private val scope: CoroutineScope,
    private val senderInfo: SenderInfo,
    private val ackTimeoutMs: Long,
    private val now: () -> Long,
) {
    private val _state = MutableStateFlow<HandoffState>(
        if (transport.isConnected.value) HandoffState.Idle else HandoffState.Disconnected,
    )

    /** Current handoff state. Phone-side UI observes this. */
    public val state: StateFlow<HandoffState> = _state.asStateFlow()

    private val pending = mutableMapOf<String, CompletableDeferred<Ack>>()
    private val pendingLock = Mutex()
    private val sendLock = Mutex()

    private val jobs: MutableList<Job> = mutableListOf()

    init {
        jobs += scope.launch {
            transport.incoming.collect { raw ->
                val decoded = runCatching { CastMessageCodec.decode(raw) }.getOrNull() ?: return@collect
                if (decoded is Ack) routeAck(decoded)
            }
        }
        jobs += scope.launch {
            transport.isConnected.collect { connected ->
                if (!connected) {
                    _state.value = HandoffState.Disconnected
                } else if (_state.value is HandoffState.Disconnected) {
                    _state.value = HandoffState.Connecting(transport.deviceName.value)
                }
            }
        }
    }

    /**
     * Send a full [HalogenThemeSpec] identified by [key]. Suspends until an
     * acknowledgment arrives or [ackTimeoutMs] elapses.
     */
    public suspend fun sendTheme(
        key: String,
        spec: HalogenThemeSpec,
        label: String? = null,
    ): HandoffResult {
        val ts = now()
        val id = generateMessageId(ts)
        val payload = ThemePayload(id = id, ts = ts, key = key, spec = spec, label = label, sender = senderInfo)
        return sendPartial(payload)
    }

    /**
     * Send a pre-built [ThemePayload]. Use this when only some theme fields
     * are known (callers omit nullable fields on [ThemePayload]). Suspends
     * until an acknowledgment arrives or [ackTimeoutMs] elapses.
     */
    public suspend fun sendPartial(payload: ThemePayload): HandoffResult {
        return sendAndAwait(payload) {
            _state.value = HandoffState.Sending(payload.key, payload.label)
        }.let { result ->
            when (result) {
                is HandoffResult.Acknowledged ->
                    _state.value = HandoffState.Acknowledged(
                        deviceName = result.deviceName,
                        key = payload.key,
                        label = payload.label,
                        at = now(),
                    )

                is HandoffResult.Failed ->
                    _state.value = HandoffState.Failed(result.reason)

                HandoffResult.TimedOut ->
                    _state.value = HandoffState.Failed("ack timeout")
            }
            result
        }
    }

    /**
     * Ask the receiver to drop the current theme. Returns the [HandoffResult]
     * of the clear message itself.
     */
    public suspend fun clearTheme(reason: ClearReason = ClearReason.SENDER_EXITED): HandoffResult {
        val ts = now()
        val id = generateMessageId(ts)
        val msg = ClearTheme(id = id, ts = ts, reason = reason)
        return sendAndAwait(msg) { /* no state transition on clear */ }
    }

    /** Cancel internal coroutines. Does not close the underlying [CastMessageTransport]. */
    public fun close() {
        jobs.forEach { it.cancel() }
        jobs.clear()
    }

    private suspend fun sendAndAwait(
        msg: CastMessage,
        beforeSend: () -> Unit,
    ): HandoffResult {
        if (!transport.isConnected.value) {
            return HandoffResult.Failed("not connected")
        }
        val deferred = CompletableDeferred<Ack>()
        pendingLock.withLock { pending[msg.id] = deferred }
        try {
            beforeSend()
            sendLock.withLock {
                try {
                    transport.send(CastMessageCodec.encode(msg))
                } catch (t: Throwable) {
                    return HandoffResult.Failed(t.message ?: "transport error")
                }
            }
            val ack = try {
                withTimeout(ackTimeoutMs) { deferred.await() }
            } catch (_: TimeoutCancellationException) {
                return HandoffResult.TimedOut
            }
            return if (ack.ok) {
                HandoffResult.Acknowledged(ack.deviceName)
            } else {
                HandoffResult.Failed(ack.error ?: "receiver rejected")
            }
        } finally {
            pendingLock.withLock { pending.remove(msg.id) }
        }
    }

    private suspend fun routeAck(ack: Ack) {
        pendingLock.withLock { pending[ack.id] }?.complete(ack)
    }
}
