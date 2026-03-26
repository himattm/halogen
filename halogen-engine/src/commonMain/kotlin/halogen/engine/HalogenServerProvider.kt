package halogen.engine

import halogen.HalogenThemeSpec

/**
 * Provider for server-side pre-generated themes.
 *
 * Implement this to fetch themes from your own backend, enabling
 * A/B testing, admin-curated themes, or offline-first workflows.
 *
 * @see Halogen.Builder.serverProvider
 */
public fun interface HalogenServerProvider {
    /**
     * Fetch a pre-generated theme for the given [key].
     *
     * @param key The theme key (e.g., a URL path or category identifier).
     * @return The theme spec, or `null` if no server-side theme exists for this key.
     */
    public suspend fun fetchTheme(key: String): HalogenThemeSpec?
}
