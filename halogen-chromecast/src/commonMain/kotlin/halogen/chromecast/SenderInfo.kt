package halogen.chromecast

import kotlinx.serialization.Serializable

/**
 * Metadata identifying the sender of a cast message. Optional on the wire;
 * useful for receivers that render multi-app surfaces or debug overlays.
 *
 * @property appId Reverse-DNS application identifier (e.g. `com.example.tunes`).
 * @property appVersion Sender app's version name.
 * @property platform One of `android`, `ios`, `jvm`, `wasmjs`.
 * @property halogenVersion Version of the Halogen library the sender is using.
 */
@Serializable
public data class SenderInfo(
    val appId: String,
    val appVersion: String,
    val platform: String,
    val halogenVersion: String,
)
