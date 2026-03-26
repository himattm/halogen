package halogen.engine

import halogen.HalogenThemeSpec

/**
 * Fetches pre-built themes from a remote source (your backend, a CMS, etc.).
 *
 * Unlike [HalogenLlmProvider][halogen.HalogenLlmProvider], this does not generate
 * themes via an LLM. It performs a simple key-value lookup, enabling use cases like
 * admin-curated themes, A/B testing, or offline-first workflows.
 *
 * @see Halogen.Builder.remoteThemes
 */
public fun interface HalogenRemoteThemes {
    /**
     * Fetch a pre-built theme for the given [key].
     *
     * @param key The theme key (e.g., a URL path or category identifier).
     * @return The theme spec, or `null` if no remote theme exists for this key.
     */
    public suspend fun fetchTheme(key: String): HalogenThemeSpec?
}
