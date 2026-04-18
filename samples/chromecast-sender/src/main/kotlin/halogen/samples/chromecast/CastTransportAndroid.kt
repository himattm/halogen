package halogen.samples.chromecast

import com.google.android.gms.cast.Cast
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManager
import com.google.android.gms.cast.framework.SessionManagerListener
import halogen.chromecast.CastMessageTransport
import halogen.chromecast.CastNamespace
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Android adapter bridging a [CastContext]'s [SessionManager] to [CastMessageTransport].
 *
 * Lifecycle: construct once per activity/app scope, attach via [install], and
 * call [uninstall] on dispose. Safe to use while a session is not yet active —
 * the transport stays `isConnected = false` until `onSessionStarted` fires.
 */
class CastTransportAndroid(
    private val castContext: CastContext,
    override val namespace: String = CastNamespace.HALOGEN,
) : CastMessageTransport {

    private val _incoming = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 32)
    override val incoming = _incoming.asSharedFlow()

    private val _connected = MutableStateFlow(false)
    override val isConnected = _connected

    private val _deviceName = MutableStateFlow<String?>(null)
    override val deviceName = _deviceName

    private var currentSession: CastSession? = null

    private val messageCallback = Cast.MessageReceivedCallback { _, ns, payload ->
        if (ns == namespace) {
            _incoming.tryEmit(payload)
        }
    }

    private val listener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, sessionId: String) = attach(session)
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) = attach(session)
        override fun onSessionEnded(session: CastSession, error: Int) = detach()
        override fun onSessionSuspended(session: CastSession, reason: Int) = detach()
        override fun onSessionStarting(session: CastSession) {}
        override fun onSessionResuming(session: CastSession, sessionId: String) {}
        override fun onSessionEnding(session: CastSession) {}
        override fun onSessionStartFailed(session: CastSession, error: Int) = detach()
        override fun onSessionResumeFailed(session: CastSession, error: Int) = detach()
    }

    /** Attach listeners. Call from [android.app.Activity.onStart] or similar. */
    fun install() {
        castContext.sessionManager.addSessionManagerListener(listener, CastSession::class.java)
        castContext.sessionManager.currentCastSession?.let(::attach)
    }

    /** Detach listeners. Call from [android.app.Activity.onStop]. */
    fun uninstall() {
        detach()
        castContext.sessionManager.removeSessionManagerListener(listener, CastSession::class.java)
    }

    private fun attach(session: CastSession) {
        currentSession = session
        try {
            session.setMessageReceivedCallbacks(namespace, messageCallback)
        } catch (_: Exception) {
            // Channel may already be registered — ignore.
        }
        _deviceName.value = session.castDevice?.friendlyName
        _connected.value = true
    }

    private fun detach() {
        try {
            currentSession?.removeMessageReceivedCallbacks(namespace)
        } catch (_: Exception) {
            // Session may already be gone.
        }
        currentSession = null
        _connected.value = false
    }

    override suspend fun send(payload: String): Unit = suspendCancellableCoroutine { cont ->
        val session = currentSession
        if (session == null) {
            cont.resumeWithException(IllegalStateException("No active Cast session"))
            return@suspendCancellableCoroutine
        }
        session.sendMessage(namespace, payload)
            .setResultCallback { status ->
                if (status.isSuccess) cont.resume(Unit)
                else cont.resumeWithException(RuntimeException("Cast send failed: ${status.statusMessage}"))
            }
    }
}
