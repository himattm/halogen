package me.mmckenna.halogen.sample.screens.playground

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import halogen.HalogenConfig
import halogen.HalogenLlmAvailability
import halogen.HalogenThemeSpec
import halogen.engine.HalogenEngine
import halogen.engine.HalogenResult
import halogen.provider.nano.GeminiNanoProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaygroundUiState(
    val prompt: String = "",
    val isLoading: Boolean = false,
    val selectedConfigName: String = "Default",
    val history: List<HalogenThemeSpec> = emptyList(),
    val processSteps: List<String> = emptyList(),
)

class PlaygroundViewModel(
    private val engine: HalogenEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaygroundUiState())
    val uiState: StateFlow<PlaygroundUiState> = _uiState.asStateFlow()

    fun setPrompt(text: String) {
        _uiState.update { it.copy(prompt = text) }
    }

    fun setSelectedConfig(name: String) {
        _uiState.update { it.copy(selectedConfigName = name) }
    }

    fun generateTheme(nanoProvider: GeminiNanoProvider) {
        val state = _uiState.value
        if (state.prompt.isBlank() || state.isLoading) return

        val config = HalogenConfig.presets[state.selectedConfigName] ?: HalogenConfig.Default
        val cacheKey = "${state.prompt}:${state.selectedConfigName}"

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, processSteps = listOf()) }

            engine.config = config
            addStep("Prompt: \"${state.prompt}\"")
            addStep("Config: ${state.selectedConfigName}")
            addStep("Checking Nano availability...")

            val availability = try {
                nanoProvider.availability()
            } catch (_: Exception) {
                HalogenLlmAvailability.UNAVAILABLE
            }
            addStep("Nano: $availability")

            addStep("Resolving theme...")
            val startTime = System.currentTimeMillis()
            val result = engine.resolve(key = cacheKey, hint = state.prompt)
            val elapsed = System.currentTimeMillis() - startTime

            when (result) {
                is HalogenResult.Success -> {
                    addStep("LLM generated in ${elapsed}ms")
                    addStep("Seeds: pri=${result.spec.primary} sec=${result.spec.secondary} ter=${result.spec.tertiary}")
                    addStep("Neutral: L=${result.spec.neutralLight} D=${result.spec.neutralDark}")
                    addStep("Style: ${result.spec.fontMood}, ${result.spec.cornerStyle} corners")
                    addStep("Expanded to M3 palette (light + dark)")
                }
                is HalogenResult.Cached -> {
                    addStep("Cache hit (${elapsed}ms)")
                    addStep("Seeds: pri=${result.spec.primary}")
                }
                is HalogenResult.ParseError -> {
                    addStep("Parse error: ${result.message}")
                    result.rawResponse?.take(100)?.let {
                        addStep("Raw: $it")
                    }
                }
                is HalogenResult.Unavailable -> {
                    addStep("All providers unavailable")
                    addStep("Using default M3 theme")
                }
                else -> addStep("Result: ${result::class.simpleName}")
            }

            result.themeSpec?.let { spec ->
                addToHistory(spec)
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun regenerateTheme(hint: String) {
        val state = _uiState.value
        if (hint.isBlank() || state.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            engine.regenerate(key = hint, hint = hint)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun applyFromHistory(spec: HalogenThemeSpec) {
        viewModelScope.launch {
            engine.apply(spec.primary, spec)
        }
    }

    private fun addStep(step: String) {
        _uiState.update { it.copy(processSteps = it.processSteps + step) }
    }

    private fun addToHistory(spec: HalogenThemeSpec) {
        _uiState.update { current ->
            val newHistory = if (current.history.contains(spec)) {
                current.history
            } else {
                (listOf(spec) + current.history).take(10)
            }
            current.copy(history = newHistory)
        }
    }

    companion object {
        fun factory(engine: HalogenEngine) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PlaygroundViewModel(engine) as T
            }
        }
    }
}
