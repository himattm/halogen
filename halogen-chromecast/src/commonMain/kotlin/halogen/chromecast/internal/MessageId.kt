package halogen.chromecast.internal

import kotlin.random.Random

/**
 * Generates an opaque, monotonic-ish message id string without pulling a UUID
 * dependency. Format: `<base36 timestamp>-<base36 random>`.
 */
internal fun generateMessageId(ts: Long): String {
    val r = Random.nextLong().toString(radix = 36).removePrefix("-")
    return ts.toString(radix = 36) + "-" + r
}
