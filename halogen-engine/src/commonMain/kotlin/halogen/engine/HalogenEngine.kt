package halogen.engine

import halogen.HalogenConfig
import halogen.HalogenExtension
import halogen.HalogenLlmAvailability
import halogen.HalogenLlmException
import halogen.HalogenLlmProvider
import halogen.HalogenThemeSpec
import halogen.PromptBuilder
import halogen.SchemaParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Main orchestrator for Halogen theme generation, caching, and application.
 *
 * Resolve chain: cache -> server -> LLM -> default.
 */
public class HalogenEngine internal constructor(
    private val providers: List<HalogenLlmProvider>,
    private val serverProvider: HalogenServerProvider?,
    private val cache: ThemeCache,
    private val defaultSpec: HalogenThemeSpec?,
    private val extensions: List<HalogenExtension>,
    /** Color science configuration. Mutable so it can be changed at runtime (e.g., config picker). */
    public var config: HalogenConfig,
    private val scope: CoroutineScope,
    private val promptInstructions: String = "",
    private val promptExamples: List<Pair<String, String>> = emptyList(),
) {
    private val _activeTheme = MutableStateFlow<HalogenThemeSpec?>(defaultSpec)

    /** The currently active [HalogenThemeSpec], or null if none is applied. */
    public val activeTheme: StateFlow<HalogenThemeSpec?> = _activeTheme.asStateFlow()

    private val _activeKey = MutableStateFlow<String?>(null)

    /** The key of the currently active theme, or null if using default or no theme. */
    public val activeKey: StateFlow<String?> = _activeKey.asStateFlow()

    /** Whether caching is enabled for resolve operations. */
    public var cachingEnabled: Boolean = true

    /**
     * Resolve a theme for the given [key].
     *
     * Resolution order:
     * 1. Cache (if [cachingEnabled])
     * 2. Server provider
     * 3. LLM providers (in registration order)
     * 4. Default spec
     *
     * @param key Unique identifier for the theme (e.g., a URL path or category).
     * @param hint Optional human-readable description to guide LLM generation.
     * @return A [HalogenResult] describing the outcome.
     */
    public suspend fun resolve(key: String, hint: String? = null): HalogenResult =
        resolveInternal(key, hint, apply = true)

    /**
     * Prefetch a theme for the given [key] without applying it.
     * Useful for warming the cache ahead of navigation.
     */
    public suspend fun prefetch(key: String, hint: String? = null): HalogenResult =
        resolveInternal(key, hint, apply = false)

    private suspend fun resolveInternal(key: String, hint: String?, apply: Boolean): HalogenResult {
        if (cachingEnabled) {
            val cached = cache.get(key)
            if (cached != null) {
                if (apply) applyInternal(key, cached)
                return HalogenResult.Cached(cached)
            }
        }

        if (serverProvider != null) {
            try {
                val serverSpec = serverProvider.fetchTheme(key)
                if (serverSpec != null) {
                    if (cachingEnabled) {
                        cache.put(key, serverSpec, ThemeSource.SERVER)
                    }
                    if (apply) applyInternal(key, serverSpec)
                    return HalogenResult.FromServer(serverSpec)
                }
            } catch (_: Exception) {
                // Server fetch failed, continue to LLM
            }
        }

        val effectiveHint = hint ?: HintExtractor.extract(key) ?: key
        val llmResult = generateFromLlm(effectiveHint)
        val llmSpec = llmResult.themeSpec
        if (llmSpec != null) {
            if (cachingEnabled) {
                cache.put(key, llmSpec, ThemeSource.LLM)
            }
            if (apply) applyInternal(key, llmSpec)
            return llmResult
        }

        if (apply && defaultSpec != null) {
            applyInternal(key, defaultSpec)
        }
        return if (llmResult is HalogenResult.ParseError) llmResult else HalogenResult.Unavailable
    }

    /**
     * Manually apply a [spec] for the given [key], caching it as [ThemeSource.MANUAL].
     */
    public suspend fun apply(key: String, spec: HalogenThemeSpec) {
        if (cachingEnabled) {
            cache.put(key, spec, ThemeSource.MANUAL)
        }
        applyInternal(key, spec)
    }

    /**
     * Reset to the default theme spec, clearing the active key.
     */
    public fun applyDefault() {
        _activeTheme.value = defaultSpec
        _activeKey.value = null
    }

    /**
     * Force-regenerate a theme from LLM, bypassing cache.
     * The result replaces any cached entry for the same key.
     *
     * @param key The theme key.
     * @param hint Human-readable description for the LLM.
     */
    public suspend fun regenerate(key: String, hint: String): HalogenResult {
        val result = generateFromLlm(hint)
        val resultSpec = result.themeSpec
        if (resultSpec != null) {
            if (cachingEnabled) {
                cache.put(key, resultSpec, ThemeSource.LLM)
            }
            applyInternal(key, resultSpec)
        }
        return result
    }

    /**
     * Refresh a theme: evict from cache and re-resolve.
     */
    public suspend fun refresh(key: String, hint: String? = null): HalogenResult {
        cache.evict(key)
        return resolve(key, hint)
    }

    /**
     * Evict a single key from the cache.
     */
    public suspend fun evict(key: String) {
        cache.evict(key)
    }

    /**
     * Evict multiple keys from the cache.
     */
    public suspend fun evict(keys: Set<String>) {
        cache.evict(keys)
    }

    /**
     * Clear all cached themes.
     */
    public suspend fun clearCache() {
        cache.clear()
    }

    private fun applyInternal(key: String, spec: HalogenThemeSpec) {
        _activeTheme.value = spec
        _activeKey.value = key
    }

    private suspend fun generateFromLlm(hint: String): HalogenResult = withContext(Dispatchers.Default) {
        val prompt = PromptBuilder.build(hint, extensions, config, promptInstructions, promptExamples)

        for (provider in providers) {
            try {
                var availability = provider.availability()

                if (availability == HalogenLlmAvailability.INITIALIZING) {
                    var waited = 0
                    while (availability == HalogenLlmAvailability.INITIALIZING && waited < 120) {
                        kotlinx.coroutines.delay(2000)
                        waited += 2
                        availability = provider.availability()
                    }
                }

                if (availability != HalogenLlmAvailability.READY) {
                    continue
                }

                val raw = provider.generate(prompt)
                val parseResult = SchemaParser.parse(raw)

                return@withContext if (parseResult.isSuccess) {
                    HalogenResult.Success(parseResult.getOrThrow())
                } else {
                    val error = parseResult.exceptionOrNull()
                    HalogenResult.ParseError(
                        message = error?.message ?: "Unknown parse error",
                        rawResponse = raw,
                    )
                }
            } catch (_: HalogenLlmException) {
                continue
            } catch (_: Exception) {
                continue
            }
        }

        return@withContext HalogenResult.Unavailable
    }
}
