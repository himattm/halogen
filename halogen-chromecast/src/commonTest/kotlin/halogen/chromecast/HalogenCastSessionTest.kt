package halogen.chromecast

import halogen.HalogenThemeSpec
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HalogenCastSessionTest {

    private val senderInfo = SenderInfo("app", "1.0", "jvm", "0.2.0")

    private fun sampleSpec() = HalogenThemeSpec(
        primary = "#112233", secondary = "#445566", tertiary = "#778899",
        neutralLight = "#FFFFFF", neutralDark = "#000000", error = "#FF0000",
        fontMood = "modern", headingWeight = 500, bodyWeight = 400,
        tightLetterSpacing = false, cornerStyle = "rounded", cornerScale = 1f,
    )

    @Test
    fun sendTheme_happyPath_reachesAcknowledged() = runTest {
        val transport = FakeTransport()
        val session = HalogenCastSession(transport, this, senderInfo, 2_000, now = { currentTime })

        val result = async {
            session.sendTheme("track:1", sampleSpec(), label = "Song A")
        }
        advanceUntilIdle()

        val raw = transport.sent.receive()
        val sent = CastMessageCodec.decode(raw)
        assertIs<ThemePayload>(sent)
        assertEquals("track:1", sent.key)
        assertEquals("Song A", sent.label)

        transport.deliver(Ack(id = sent.id, ts = 1L, deviceName = "TV"))
        advanceUntilIdle()

        assertEquals(HandoffResult.Acknowledged("TV"), result.await())
        val state = session.state.value
        assertIs<HandoffState.Acknowledged>(state)
        assertEquals("TV", state.deviceName)
        assertEquals("track:1", state.key)

        session.close()
    }

    @Test
    fun sendTheme_ackTimeout_reportsTimedOutAndFailedState() = runTest {
        val transport = FakeTransport()
        val session = HalogenCastSession(transport, this, senderInfo, ackTimeoutMs = 1_000L, now = { currentTime })

        val result = async { session.sendTheme("k", sampleSpec()) }
        advanceTimeBy(1_500L)
        advanceUntilIdle()

        assertEquals(HandoffResult.TimedOut, result.await())
        val state = session.state.value
        assertIs<HandoffState.Failed>(state)
        assertTrue(state.reason.contains("timeout"))
        session.close()
    }

    @Test
    fun ackForUnknownId_isIgnored() = runTest {
        val transport = FakeTransport()
        val session = HalogenCastSession(transport, this, senderInfo, ackTimeoutMs = 1_000L, now = { currentTime })

        val result = async { session.sendTheme("k", sampleSpec()) }
        advanceUntilIdle()
        transport.sent.receive() // consume

        transport.deliver(Ack(id = "not-our-id", ts = 0L, deviceName = "TV"))
        advanceTimeBy(1_200L)
        advanceUntilIdle()

        assertEquals(HandoffResult.TimedOut, result.await())
        session.close()
    }

    @Test
    fun disconnect_flipsState() = runTest {
        val transport = FakeTransport()
        val session = HalogenCastSession(transport, this, senderInfo, 1_000L, now = { currentTime })
        advanceUntilIdle()
        assertEquals(HandoffState.Idle, session.state.value)

        transport.isConnected.value = false
        advanceUntilIdle()
        assertEquals(HandoffState.Disconnected, session.state.value)

        transport.isConnected.value = true
        advanceUntilIdle()
        assertIs<HandoffState.Connecting>(session.state.value)
        session.close()
    }

    @Test
    fun send_whenDisconnected_returnsFailed() = runTest {
        val transport = FakeTransport(initialConnected = false)
        val session = HalogenCastSession(transport, this, senderInfo, 1_000L, now = { currentTime })
        advanceUntilIdle()

        val r = session.sendTheme("k", sampleSpec())
        assertIs<HandoffResult.Failed>(r)
        session.close()
    }

    @Test
    fun minimalPayload_viaSendPartial() = runTest {
        val transport = FakeTransport()
        val session = HalogenCastSession(transport, this, senderInfo, 1_000L, now = { currentTime })

        val payload = ThemePayload(id = "x", ts = 0L, key = "k", primary = "#FF0000")
        val r = async { session.sendPartial(payload) }
        advanceUntilIdle()

        val raw = transport.sent.receive()
        val decoded = CastMessageCodec.decode(raw) as ThemePayload
        assertEquals("#FF0000", decoded.primary)
        // All other fields must be null on wire when unset.
        assertEquals(null, decoded.secondary)
        assertEquals(null, decoded.neutralLight)

        transport.deliver(Ack(id = "x", ts = 0L, deviceName = "TV"))
        advanceUntilIdle()
        assertEquals(HandoffResult.Acknowledged("TV"), r.await())
        session.close()
    }

    @Test
    fun transportSendFailure_reportsFailedState() = runTest {
        val transport = FakeTransport().apply { sendFailure = RuntimeException("boom") }
        val session = HalogenCastSession(transport, this, senderInfo, 1_000L, now = { currentTime })

        val r = session.sendTheme("k", sampleSpec())
        assertIs<HandoffResult.Failed>(r)
        assertTrue((r as HandoffResult.Failed).reason.contains("boom"))
        session.close()
    }

    @Test
    fun receiverRejection_isReportedAsFailed() = runTest {
        val transport = FakeTransport()
        val session = HalogenCastSession(transport, this, senderInfo, 2_000L, now = { currentTime })

        val r = async { session.sendTheme("k", sampleSpec()) }
        advanceUntilIdle()
        val sent = CastMessageCodec.decode(transport.sent.receive()) as ThemePayload
        transport.deliver(Ack(id = sent.id, ts = 0L, deviceName = "TV", ok = false, error = "unsupported_version"))
        advanceUntilIdle()

        val result = r.await()
        assertIs<HandoffResult.Failed>(result)
        assertEquals("unsupported_version", result.reason)
        session.close()
    }

    @Test
    fun clearTheme_sendsClearMessage() = runTest {
        val transport = FakeTransport()
        val session = HalogenCastSession(transport, this, senderInfo, 1_000L, now = { currentTime })

        val deferred = async { session.clearTheme(ClearReason.MEDIA_ENDED) }
        advanceUntilIdle()

        val decoded = CastMessageCodec.decode(transport.sent.receive())
        assertIs<ClearTheme>(decoded)
        assertEquals(ClearReason.MEDIA_ENDED, decoded.reason)

        transport.deliver(Ack(id = decoded.id, ts = 0L, deviceName = "TV"))
        advanceUntilIdle()
        assertIs<HandoffResult.Acknowledged>(deferred.await())
        session.close()
    }
}
