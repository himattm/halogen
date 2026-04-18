package halogen.samples.chromecast

import android.content.Context
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider

/**
 * Wires the demo sender to a receiver App ID. Override via the
 * `HALOGEN_CAST_APP_ID` Gradle property / env var at build time.
 */
class CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions =
        CastOptions.Builder()
            .setReceiverApplicationId(BuildConfig.CAST_APP_ID)
            .build()

    override fun getAdditionalSessionProviders(context: Context): MutableList<SessionProvider>? = null
}
