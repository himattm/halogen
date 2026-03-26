package halogen.engine

import halogen.HalogenThemeSpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A portable bundle of themes that can be exported/imported as JSON.
 */
@Serializable
public data class ThemeBundle(
    val version: Int = 1,
    val exportedAt: Long,
    val themes: Map<String, HalogenThemeSpec>,
)

private val bundleJson = Json { ignoreUnknownKeys = true; prettyPrint = false }

/**
 * Export all cached themes as a portable [ThemeBundle] JSON string.
 */
public suspend fun HalogenEngine.exportThemes(): String {
    val keys = cache.keys()
    val themes = keys.associateWith { key ->
        cache.get(key) ?: error("Cache returned null for known key: $key")
    }
    val bundle = ThemeBundle(
        version = 1,
        exportedAt = currentTimeMillis(),
        themes = themes,
    )
    return bundleJson.encodeToString(bundle)
}

/**
 * Import themes from a [ThemeBundle] JSON string into the cache.
 *
 * @return the number of themes imported.
 */
public suspend fun HalogenEngine.importThemes(json: String): Int {
    val bundle = bundleJson.decodeFromString<ThemeBundle>(json)
    for ((key, spec) in bundle.themes) {
        cache.put(key, spec, ThemeSource.MANUAL)
    }
    return bundle.themes.size
}

/**
 * Export a single theme by [key] as a [ThemeBundle] JSON string.
 *
 * @return the JSON string, or null if the key is not in the cache.
 */
public suspend fun HalogenEngine.exportTheme(key: String): String? {
    val spec = cache.get(key) ?: return null
    val bundle = ThemeBundle(
        version = 1,
        exportedAt = currentTimeMillis(),
        themes = mapOf(key to spec),
    )
    return bundleJson.encodeToString(bundle)
}

/**
 * Import a single theme from a [ThemeBundle] JSON string.
 *
 * The theme is stored under the given [key], regardless of the key(s) in the bundle.
 */
public suspend fun HalogenEngine.importTheme(key: String, json: String) {
    val bundle = bundleJson.decodeFromString<ThemeBundle>(json)
    val spec = bundle.themes.values.firstOrNull()
        ?: error("ThemeBundle contains no themes")
    cache.put(key, spec, ThemeSource.MANUAL)
}
