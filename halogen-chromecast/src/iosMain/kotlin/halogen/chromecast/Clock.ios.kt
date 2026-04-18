package halogen.chromecast

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

internal actual fun halogenCastNow(): Long =
    (NSDate().timeIntervalSince1970 * 1000).toLong()
