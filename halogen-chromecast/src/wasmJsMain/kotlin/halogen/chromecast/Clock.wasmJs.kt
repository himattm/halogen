package halogen.chromecast

private fun dateNow(): Double = js("Date.now()")

internal actual fun halogenCastNow(): Long = dateNow().toLong()
