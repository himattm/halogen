package halogen.engine

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import halogen.HalogenThemeSpec
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

private val KEY_LAST_THEME = stringPreferencesKey("last_active_theme")

/** Stored application context for DataStore file path resolution. */
private var appContext: Context? = null

/** Singleton DataStore instance — DataStore requires exactly one per file. */
private var dataStoreInstance: DataStore<Preferences>? = null

/**
 * Initialize the Halogen engine for Android. Must be called before building
 * a [HalogenEngine] (typically in `Application.onCreate()` or `Activity.onCreate()`).
 *
 * If using `halogen-cache-room`, `HalogenRoomCache.initialize(context)` calls this
 * automatically.
 */
public fun initHalogen(context: Context) {
    appContext = context.applicationContext
}

private fun getDataStore(): DataStore<Preferences> {
    return dataStoreInstance ?: run {
        val ctx = appContext ?: error(
            "initHalogen(context) must be called before building a HalogenEngine on Android.",
        )
        val path = ctx.filesDir.resolve("datastore/halogen_prefs.preferences_pb").absolutePath
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { path.toPath() },
        ).also { dataStoreInstance = it }
    }
}

private class DataStoreThemePersistence(
    private val dataStore: DataStore<Preferences>,
) : ThemePersistence {

    override suspend fun save(spec: HalogenThemeSpec) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_THEME] = HalogenThemeSpec.toJson(spec)
        }
    }

    override suspend fun load(): HalogenThemeSpec? {
        val json = dataStore.data.map { it[KEY_LAST_THEME] }.first()
        return json?.let {
            try { HalogenThemeSpec.fromJson(it) } catch (_: Exception) { null }
        }
    }

    override fun loadInitial(): HalogenThemeSpec? = kotlinx.coroutines.runBlocking { load() }
}

internal actual fun createDefaultPersistence(): ThemePersistence =
    DataStoreThemePersistence(getDataStore())
