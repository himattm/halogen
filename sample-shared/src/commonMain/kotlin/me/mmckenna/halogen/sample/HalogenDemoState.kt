package me.mmckenna.halogen.sample

import halogen.HalogenDefaults
import halogen.HalogenLlmProvider
import halogen.engine.Halogen
import halogen.engine.HalogenEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.mmckenna.halogen.sample.llms.openai.OpenAiProvider

class HalogenDemoState(
    val engine: HalogenEngine,
    val scope: CoroutineScope,
    val providerName: String,
) {
    private val _darkOverride = MutableStateFlow<Boolean?>(null)
    val darkOverride: StateFlow<Boolean?> = _darkOverride.asStateFlow()

    fun toggleDarkMode(currentIsDark: Boolean) {
        _darkOverride.value = !currentIsDark
    }

    companion object {
        fun create(
            scope: CoroutineScope,
            openAiApiKey: String? = null,
            preferredProvider: HalogenLlmProvider? = null,
            preferredProviderName: String? = null,
        ): HalogenDemoState {
            val builder = Halogen.Builder()
                .defaultTheme(HalogenDefaults.materialYou())
                .scope(scope)
                .tokenBudget(Int.MAX_VALUE)

            val (provider, providerName) = when {
                preferredProvider != null ->
                    preferredProvider to (preferredProviderName ?: "Custom")
                !openAiApiKey.isNullOrBlank() ->
                    OpenAiProvider(openAiApiKey) to "OpenAI"
                else ->
                    DemoProvider() to "Demo"
            }
            builder.provider(provider)

            return HalogenDemoState(engine = builder.build(), scope = scope, providerName = providerName)
        }
    }
}
