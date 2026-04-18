package halogen.samples.chromecast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.cast.framework.CastContext
import halogen.chromecast.HalogenCastSession
import halogen.chromecast.SenderInfo
import halogen.chromecast.handoffToChromecast
import halogen.engine.Halogen
import halogen.engine.HalogenEngine
import halogen.engine.HalogenRemoteThemes
import halogen.engine.MemoryThemeCache
import kotlinx.coroutines.MainScope

class MainActivity : ComponentActivity() {

    private lateinit var engine: HalogenEngine
    private lateinit var transport: CastTransportAndroid
    private lateinit var handoff: HalogenCastSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val demoByKey = DemoThemes.ALL.associateBy { it.name }
        engine = Halogen.Builder()
            .cache(MemoryThemeCache())
            .scope(MainScope())
            .defaultTheme(DemoThemes.ALL.first().spec)
            .remoteThemes(HalogenRemoteThemes { key -> demoByKey[key]?.spec })
            .build()

        val castContext = CastContext.getSharedInstance(this)
        transport = CastTransportAndroid(castContext)
        handoff = engine.handoffToChromecast(
            transport = transport,
            scope = lifecycleScope,
            senderInfo = SenderInfo(
                appId = packageName,
                appVersion = BuildConfig.VERSION_NAME,
                platform = "android",
                halogenVersion = "0.2.0",
            ),
        )

        setContent { DemoApp(engine = engine, handoff = handoff) }
    }

    override fun onStart() {
        super.onStart()
        transport.install()
    }

    override fun onStop() {
        transport.uninstall()
        super.onStop()
    }

    override fun onDestroy() {
        handoff.close()
        super.onDestroy()
    }
}
