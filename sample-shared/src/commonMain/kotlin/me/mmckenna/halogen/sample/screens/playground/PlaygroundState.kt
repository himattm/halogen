package me.mmckenna.halogen.sample.screens.playground

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import halogen.HalogenConfig
import halogen.HalogenThemeSpec
import halogen.engine.HalogenEngine
import halogen.engine.HalogenResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.measureTimedValue

data class PlaygroundUiState(
    val prompt: String = "",
    val isLoading: Boolean = false,
    val selectedConfigName: String = "Default",
    val history: List<HalogenThemeSpec> = emptyList(),
    val processSteps: List<String> = emptyList(),
)

class PlaygroundState(
    private val engine: HalogenEngine,
    private val scope: CoroutineScope,
) {
    private val _uiState = MutableStateFlow(PlaygroundUiState())
    val uiState: StateFlow<PlaygroundUiState> = _uiState.asStateFlow()

    // LLM tuning parameters (exposed for UI display)
    var temperature by mutableFloatStateOf(0.3f)
        private set
    var topK by mutableIntStateOf(10)
        private set
    var topP by mutableFloatStateOf(0.9f)
        private set
    var topPEnabled by mutableStateOf(false)
        private set
    var maxTokens by mutableIntStateOf(300)
        private set
    var showTuningControls by mutableStateOf(false)
        private set

    fun updateTemperature(value: Float) {
        temperature = value
    }

    fun updateTopK(value: Int) {
        topK = value
    }

    fun updateTopP(value: Float) {
        topP = value
    }

    fun updateTopPEnabled(enabled: Boolean) {
        topPEnabled = enabled
    }

    fun updateMaxTokens(value: Int) {
        maxTokens = value
    }

    fun toggleTuningControls() {
        showTuningControls = !showTuningControls
    }

    fun setPrompt(text: String) {
        _uiState.update { it.copy(prompt = text) }
    }

    fun setSelectedConfig(name: String) {
        _uiState.update { it.copy(selectedConfigName = name) }
    }

    fun generateTheme() {
        val state = _uiState.value
        if (state.prompt.isBlank() || state.isLoading) return

        val config = HalogenConfig.presets[state.selectedConfigName] ?: HalogenConfig.Default
        val cacheKey = "${state.prompt}:${state.selectedConfigName}"

        scope.launch {
            _uiState.update { it.copy(isLoading = true, processSteps = listOf()) }

            engine.config = config
            addStep("Prompt: \"${state.prompt}\"")
            addStep("Config: ${state.selectedConfigName}")
            addStep("Resolving theme...")

            val (result, duration) = measureTimedValue {
                engine.resolve(key = cacheKey, hint = state.prompt)
            }
            val elapsed = duration.inWholeMilliseconds

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

        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            engine.regenerate(key = hint, hint = hint)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun applyFromHistory(spec: HalogenThemeSpec) {
        scope.launch {
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
}
