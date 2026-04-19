package halogen.chromecast

import halogen.HalogenThemeSpec
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Current protocol version used by the library. */
public const val HALOGEN_CAST_PROTOCOL_VERSION: Int = 1

/**
 * Versioned envelope for every message exchanged on [CastNamespace.HALOGEN].
 *
 * All concrete messages carry a [v] protocol version, a unique [id] that the
 * receiver echoes in [Ack], and a [ts] send timestamp in epoch millis.
 */
@Serializable
public sealed interface CastMessage {
    /** Protocol version. Receivers must reject messages with `v` > what they support. */
    public val v: Int

    /** Unique identifier for this message; echoed by the receiver in [Ack]. */
    public val id: String

    /** Send time, epoch millis. */
    public val ts: Long
}

/**
 * Reasons a sender may clear a theme from the receiver.
 */
@Serializable
public enum class ClearReason {
    /** Sender app exited or disconnected the cast session. */
    @SerialName("sender_exited") SENDER_EXITED,

    /** Media playback ended naturally. */
    @SerialName("media_ended") MEDIA_ENDED,

    /** User explicitly asked to clear the palette. */
    @SerialName("user_cleared") USER_CLEARED,
}

/**
 * Initial handshake from the sender. Receivers should reply with [Ack] using
 * the same [id] and their supported protocol version.
 */
@Serializable
@SerialName("handshake")
public data class Handshake(
    override val v: Int = HALOGEN_CAST_PROTOCOL_VERSION,
    override val id: String,
    override val ts: Long,
    val sender: SenderInfo,
    val supportedVersions: List<Int> = listOf(HALOGEN_CAST_PROTOCOL_VERSION),
) : CastMessage

/**
 * Theme migration payload. Only [key] and [primary] are required — every other
 * color and hint is nullable so senders may send a partial palette and let the
 * receiver fill defaults or derive missing roles from [primary].
 *
 * @property key Theme identifier (track id, URL, category, etc.) — echoed in the receiver's UI and [Ack].
 * @property primary Required hex seed color (e.g. `#E53935`). Used to derive missing neighbors.
 * @property label Optional human-readable label describing the theme (e.g. `"Coffee Shop"`).
 * @property sender Optional sender metadata.
 * @property extensions Opaque `Map<String,String>` passthrough for app-specific data.
 */
@Serializable
@SerialName("theme")
public data class ThemePayload(
    override val v: Int = HALOGEN_CAST_PROTOCOL_VERSION,
    override val id: String,
    override val ts: Long,
    val key: String,
    @SerialName("pri") val primary: String,
    @SerialName("sec") val secondary: String? = null,
    @SerialName("ter") val tertiary: String? = null,
    @SerialName("neuL") val neutralLight: String? = null,
    @SerialName("neuD") val neutralDark: String? = null,
    @SerialName("err") val error: String? = null,
    @SerialName("font") val fontMood: String? = null,
    @SerialName("hw") val headingWeight: Int? = null,
    @SerialName("bw") val bodyWeight: Int? = null,
    @SerialName("ls") val tightLetterSpacing: Boolean? = null,
    @SerialName("cs") val cornerStyle: String? = null,
    @SerialName("cx") val cornerScale: Float? = null,
    val label: String? = null,
    val sender: SenderInfo? = null,
    val extensions: Map<String, String>? = null,
) : CastMessage

/**
 * Receiver-to-sender acknowledgment. [id] must match the message being acked.
 *
 * @property deviceName Friendly name of the receiver device (e.g. `"Living Room TV"`).
 * @property negotiatedVersion Protocol version the receiver will honor.
 * @property ok `true` on success, `false` if the receiver rejected the message.
 * @property error Optional machine-readable error code when `ok == false`.
 */
@Serializable
@SerialName("ack")
public data class Ack(
    override val v: Int = HALOGEN_CAST_PROTOCOL_VERSION,
    override val id: String,
    override val ts: Long,
    val deviceName: String,
    val negotiatedVersion: Int = HALOGEN_CAST_PROTOCOL_VERSION,
    val ok: Boolean = true,
    val error: String? = null,
) : CastMessage

/**
 * Ask the receiver to revert to its default theme.
 */
@Serializable
@SerialName("clear")
public data class ClearTheme(
    override val v: Int = HALOGEN_CAST_PROTOCOL_VERSION,
    override val id: String,
    override val ts: Long,
    val reason: ClearReason = ClearReason.SENDER_EXITED,
) : CastMessage

/**
 * Build a [ThemePayload] from an existing [HalogenThemeSpec].
 */
public fun ThemePayload(
    id: String,
    ts: Long,
    key: String,
    spec: HalogenThemeSpec,
    label: String? = null,
    sender: SenderInfo? = null,
): ThemePayload = ThemePayload(
    id = id,
    ts = ts,
    key = key,
    primary = spec.primary,
    secondary = spec.secondary,
    tertiary = spec.tertiary,
    neutralLight = spec.neutralLight,
    neutralDark = spec.neutralDark,
    error = spec.error,
    fontMood = spec.fontMood,
    headingWeight = spec.headingWeight,
    bodyWeight = spec.bodyWeight,
    tightLetterSpacing = spec.tightLetterSpacing,
    cornerStyle = spec.cornerStyle,
    cornerScale = spec.cornerScale,
    label = label,
    sender = sender,
    extensions = spec.extensions,
)
