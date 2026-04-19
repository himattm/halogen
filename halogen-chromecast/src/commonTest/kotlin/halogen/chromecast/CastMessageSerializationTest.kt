package halogen.chromecast

import halogen.HalogenThemeSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CastMessageSerializationTest {

    private val sender = SenderInfo(
        appId = "com.example.tunes",
        appVersion = "3.4.1",
        platform = "android",
        halogenVersion = "0.2.0",
    )

    @Test
    fun handshake_roundTrip() {
        val h = Handshake(id = "abc", ts = 123L, sender = sender)
        val round = CastMessageCodec.decode(CastMessageCodec.encode(h))
        assertEquals(h, round)
    }

    @Test
    fun ack_roundTrip() {
        val a = Ack(id = "abc", ts = 1L, deviceName = "Living Room TV", ok = true)
        assertEquals(a, CastMessageCodec.decode(CastMessageCodec.encode(a)))
    }

    @Test
    fun clearTheme_roundTrip() {
        val c = ClearTheme(id = "q", ts = 1L, reason = ClearReason.USER_CLEARED)
        assertEquals(c, CastMessageCodec.decode(CastMessageCodec.encode(c)))
    }

    @Test
    fun themePayload_fullSpec_roundTrip() {
        val spec = HalogenThemeSpec(
            primary = "#E53935",
            secondary = "#FFB300",
            tertiary = "#43A047",
            neutralLight = "#FAF8F5",
            neutralDark = "#1A1A1A",
            error = "#B00020",
            fontMood = "bold",
            headingWeight = 700,
            bodyWeight = 400,
            tightLetterSpacing = true,
            cornerStyle = "rounded",
            cornerScale = 1.0f,
        )
        val msg = ThemePayload(id = "m1", ts = 0L, key = "track:1", spec = spec, label = "Song", sender = sender)
        val raw = CastMessageCodec.encode(msg)
        val decoded = CastMessageCodec.decode(raw)
        assertEquals(msg, decoded)
    }

    @Test
    fun themePayload_minimalRequiredFields_decodes() {
        val raw = """{"type":"theme","v":1,"id":"m2","ts":0,"key":"k","pri":"#FF0000"}"""
        val decoded = CastMessageCodec.decode(raw)
        assertIs<ThemePayload>(decoded)
        assertEquals("k", decoded.key)
        assertEquals("#FF0000", decoded.primary)
        assertNull(decoded.secondary)
        assertNull(decoded.label)
        assertNull(decoded.sender)
    }

    @Test
    fun themePayload_unknownFields_areIgnored() {
        val raw = """{"type":"theme","v":1,"id":"m3","ts":0,"key":"k","pri":"#FF0000","nonce":"xyz","future":42}"""
        val decoded = CastMessageCodec.decode(raw)
        assertIs<ThemePayload>(decoded)
        assertEquals("k", decoded.key)
    }

    @Test
    fun encoded_themePayload_usesTypeDiscriminatorAndShortKeys() {
        val msg = ThemePayload(id = "m", ts = 0L, key = "k", primary = "#FF0000")
        val raw = CastMessageCodec.encode(msg)
        assertTrue(raw.contains("\"type\":\"theme\""), raw)
        assertTrue(raw.contains("\"pri\":\"#FF0000\""), raw)
    }

    @Test
    fun classDiscriminator_onEncode_forEachType() {
        assertNotNull(CastMessageCodec.encode(Handshake(id = "a", ts = 0L, sender = sender)).let {
            check(it.contains("\"type\":\"handshake\"")) { it }
        })
        assertNotNull(CastMessageCodec.encode(Ack(id = "a", ts = 0L, deviceName = "d")).let {
            check(it.contains("\"type\":\"ack\"")) { it }
        })
        assertNotNull(CastMessageCodec.encode(ClearTheme(id = "a", ts = 0L)).let {
            check(it.contains("\"type\":\"clear\"")) { it }
        })
    }
}
