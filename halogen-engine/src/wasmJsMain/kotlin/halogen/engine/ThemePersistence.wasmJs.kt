package halogen.engine

import halogen.HalogenThemeSpec

private const val KEY_LAST_THEME = "halogen_last_active_theme"

@JsFun("(key, value) => window.localStorage.setItem(key, value)")
private external fun lsSetItem(key: JsString, value: JsString)

@JsFun("(key) => window.localStorage.getItem(key)")
private external fun lsGetItem(key: JsString): JsString?

private class LocalStorageThemePersistence : ThemePersistence {

    override suspend fun save(spec: HalogenThemeSpec) {
        lsSetItem(KEY_LAST_THEME.toJsString(), HalogenThemeSpec.toJson(spec).toJsString())
    }

    override suspend fun load(): HalogenThemeSpec? = loadInitial()

    override fun loadInitial(): HalogenThemeSpec? {
        val json = lsGetItem(KEY_LAST_THEME.toJsString())?.toString() ?: return null
        return try {
            HalogenThemeSpec.fromJson(json)
        } catch (_: Exception) {
            null
        }
    }
}

internal actual fun createDefaultPersistence(): ThemePersistence = LocalStorageThemePersistence()
