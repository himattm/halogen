package halogen.chromecast

/**
 * Cast custom-channel namespace constants used by this library.
 *
 * A Chromecast namespace is a string of the form `urn:x-cast:<reverse-dns>` that
 * identifies a custom message channel between a sender (phone) and a receiver
 * (TV app). Any sender and any receiver that agree on the same namespace can
 * exchange messages, regardless of which Cast application ID is hosting the
 * receiver.
 *
 * This module defaults to a single shared namespace so every Halogen-powered
 * sender can talk to every Halogen-aware receiver. Apps that need isolation
 * may override via [HalogenCastSession]'s namespace parameter.
 */
public object CastNamespace {

    /** Shared Halogen Cast namespace. */
    public const val HALOGEN: String = "urn:x-cast:me.mmckenna.halogen"
}
