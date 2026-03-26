package halogen.engine

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import halogen.HalogenThemeSpec
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

private val KEY_LAST_THEME = stringPreferencesKey("last_active_theme")

/** Singleton DataStore instance — DataStore requires exactly one per file. */
private var dataStoreInstance: DataStore<Preferences>? = null

private fun getDataStore(): DataStore<Preferences> {
    return dataStoreInstance ?: run {
        val urls = NSFileManager.defaultManager.URLsForDirectory(
            NSDocumentDirectory, NSUserDomainMask,
        )
        @Suppress("UNCHECKED_CAST")
        val docDir = (urls as List<NSURL>).first().path ?: ""
        val path = "$docDir/halogen_prefs.preferences_pb"
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
