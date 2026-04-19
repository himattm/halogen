package halogen.chromecast

import kotlinx.serialization.json.Json

/**
 * JSON codec for [CastMessage] envelopes.
 *
 * Uses a `"type"` class discriminator to distinguish between message kinds on
 * the wire and allows unknown keys so new optional fields are non-breaking.
 */
public object CastMessageCodec {

    /** Underlying [Json] configured for the Halogen Cast protocol. */
    public val json: Json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
        encodeDefaults = true
        isLenient = true
    }

    /** Encode a [CastMessage] to its JSON wire representation. */
    public fun encode(msg: CastMessage): String = json.encodeToString(CastMessage.serializer(), msg)

    /** Decode a JSON string to a [CastMessage]. Throws on malformed input. */
    public fun decode(raw: String): CastMessage = json.decodeFromString(CastMessage.serializer(), raw)
}
