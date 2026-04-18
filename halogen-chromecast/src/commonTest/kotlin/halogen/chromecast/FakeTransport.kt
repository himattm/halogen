package halogen.chromecast

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [CastMessageTransport] for tests. Captures sends into [sent] and
 * lets tests push receiver payloads through [deliver].
 */
class FakeTransport(
    override val namespace: String = CastNamespace.HALOGEN,
    initialConnected: Boolean = true,
    initialDeviceName: String? = "Living Room TV",
) : CastMessageTransport {
    private val _incoming = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 16)
    override val incoming = _incoming

    override val isConnected = MutableStateFlow(initialConnected)
    override val deviceName = MutableStateFlow(initialDeviceName)

    val sent: Channel<String> = Channel(capacity = Channel.UNLIMITED)

    var sendFailure: Throwable? = null

    override suspend fun send(payload: String) {
        sendFailure?.let { throw it }
        sent.send(payload)
    }

    /** Push a raw JSON payload into [incoming] as if the receiver sent it. */
    suspend fun deliver(raw: String) {
        _incoming.emit(raw)
    }

    suspend fun deliver(msg: CastMessage) = deliver(CastMessageCodec.encode(msg))
}
