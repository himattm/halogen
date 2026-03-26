package halogen.engine

import halogen.HalogenConfig
import halogen.HalogenExtension
import halogen.HalogenLlmProvider
import halogen.HalogenThemeSpec
import halogen.PromptBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Entry point and builder for configuring a [HalogenEngine] instance.
 *
 * Usage:
 * ```
 * val engine = Halogen.Builder()
 *     .provider(myLlmProvider)
 *     .defaultTheme(fallbackSpec)
 *     .build()
 * ```
 */
public class Halogen private constructor() {

    public class Builder {
        private var provider: HalogenLlmProvider? = null
        private var remoteThemes: HalogenRemoteThemes? = null
        private var cache: ThemeCache = MemoryThemeCache()
        private var defaultSpec: HalogenThemeSpec? = null
        private val extensions = mutableListOf<HalogenExtension>()
        private var config: HalogenConfig = HalogenConfig.Default
        private var scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private var promptInstructions: String = ""
        private var promptExamples: List<Pair<String, String>> = emptyList()
        private var tokenBudget: Int = DEFAULT_TOKEN_BUDGET
        private var disablePersistence: Boolean = false

        /** Disable last-theme persistence. Used internally for testing. */
        internal fun noPersistence(): Builder = apply { disablePersistence = true }

        /**
         * Set the LLM provider for theme generation.
         *
         * This can be an on-device provider like [GeminiNanoProvider][halogen.provider.nano.GeminiNanoProvider]
         * or any cloud LLM (OpenAI, Claude, Ollama, etc.) that implements [HalogenLlmProvider].
         */
        public fun provider(provider: HalogenLlmProvider): Builder = apply {
            this.provider = provider
        }

        /**
         * Set a remote theme source for pre-built themes.
         *
         * Unlike [provider], this does not use an LLM. It fetches pre-generated themes
         * by key from your backend, CMS, or any remote source. Useful for admin-curated
         * themes, A/B testing, or offline-first workflows.
         *
         * Resolution order: cache -> remoteThemes -> LLM provider -> default.
         */
        public fun remoteThemes(source: HalogenRemoteThemes): Builder = apply {
            this.remoteThemes = source
        }

        /**
         * Set the cache implementation. Defaults to [MemoryThemeCache].
         */
        public fun cache(cache: ThemeCache): Builder = apply {
            this.cache = cache
        }

        /**
         * Set the default theme spec used when all providers are unavailable.
         */
        public fun defaultTheme(spec: HalogenThemeSpec): Builder = apply {
            this.defaultSpec = spec
        }

        /**
         * Set custom extension tokens to include in LLM prompts.
         */
        public fun extensions(vararg extensions: HalogenExtension): Builder = apply {
            this.extensions.clear()
            this.extensions.addAll(extensions)
        }

        /**
         * Set the color science configuration. Controls chroma levels, neutral tinting,
         * and error palette behavior. See [HalogenConfig] for details.
         *
         * Defaults to [HalogenConfig.Default] (M3 SchemeTonalSpot).
         * Use [HalogenConfig.Vibrant] for bolder colors or [HalogenConfig.Muted] for subdued.
         */
        public fun config(config: HalogenConfig): Builder = apply {
            this.config = config
        }

        /**
         * Set the [CoroutineScope] for background work.
         */
        public fun scope(scope: CoroutineScope): Builder = apply {
            this.scope = scope
        }

        /**
         * Add custom instructions appended to the system prompt.
         * These guide the LLM's aesthetic choices without modifying the JSON schema contract.
         *
         * @param instructions Additional rules for the LLM (e.g., brand constraints, domain context).
         */
        public fun promptInstructions(instructions: String): Builder = apply {
            this.promptInstructions = instructions
        }

        /**
         * Add custom few-shot examples appended after the built-in examples.
         * Each pair is (input description, expected JSON output).
         *
         * @param examples Pairs of (user hint, expected JSON response).
         */
        public fun promptExamples(vararg examples: Pair<String, String>): Builder = apply {
            this.promptExamples = examples.toList()
        }

        /**
         * Set the maximum estimated input token budget for the assembled prompt.
         * [build] throws [IllegalStateException] if the prompt exceeds this budget.
         *
         * Defaults to [DEFAULT_TOKEN_BUDGET] (4,000 - Gemini Nano's limit).
         * Cloud providers with larger context windows can increase this or
         * pass [Int.MAX_VALUE] to disable validation.
         */
        public fun tokenBudget(tokens: Int): Builder = apply {
            this.tokenBudget = tokens
        }

        /**
         * Build the [HalogenEngine] with the current configuration.
         *
         * @throws IllegalStateException if no provider or remoteThemes source is configured.
         * @throws IllegalStateException if the estimated prompt exceeds the [tokenBudget].
         */
        public fun build(): HalogenEngine {
            require(provider != null || remoteThemes != null) {
                "At least one theme source must be configured. " +
                    "Call .provider() or .remoteThemes() before .build()."
            }

            val persistence = if (disablePersistence) null else try {
                createDefaultPersistence()
            } catch (_: Exception) {
                null
            }

            val engine = HalogenEngine(
                provider = provider,
                remoteThemes = remoteThemes,
                cache = cache,
                defaultSpec = defaultSpec,
                extensions = extensions.toList(),
                config = config,
                scope = scope,
                promptInstructions = promptInstructions,
                promptExamples = promptExamples,
                persistence = persistence,
            )

            // Token budget validation
            if (provider != null && tokenBudget < Int.MAX_VALUE) {
                val samplePrompt = PromptBuilder.build(
                    userHint = "test theme description",
                    extensions = extensions.toList(),
                    config = config,
                    additionalInstructions = promptInstructions,
                    customExamples = promptExamples,
                )
                val estimatedTokens = PromptBuilder.estimateTokenCount(samplePrompt)
                if (estimatedTokens > tokenBudget) {
                    val breakdown = PromptBuilder.tokenBreakdown(
                        extensions = extensions.toList(),
                        config = config,
                        additionalInstructions = promptInstructions,
                        customExamples = promptExamples,
                    )
                    throw IllegalStateException(
                        "Estimated prompt exceeds token budget ($estimatedTokens / $tokenBudget tokens)\n$breakdown\n" +
                            "Reduce promptInstructions or promptExamples, or increase tokenBudget.",
                    )
                }
            }

            return engine
        }

        public companion object {
            /** Default token budget matching Gemini Nano's ~4,000 input token limit. */
            public const val DEFAULT_TOKEN_BUDGET: Int = 4000
        }
    }
}
