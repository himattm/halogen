package me.mmckenna.halogen.sample

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import halogen.HalogenDefaults
import halogen.HalogenLlmAvailability
import halogen.engine.Halogen
import halogen.engine.HalogenCache
import halogen.engine.HalogenEngine
import halogen.provider.nano.GeminiNanoProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.mmckenna.halogen.sample.llms.LlmProviderChoice
import me.mmckenna.halogen.sample.llms.openai.OpenAiProvider

class HalogenAppViewModel : ViewModel() {
    // Providers
    val nanoProvider = GeminiNanoProvider()
    private val openAiKey = BuildConfig.OPENAI_API_KEY
    val openAiAvailable = openAiKey.isNotBlank()

    // Provider selection
    private val _selectedProvider = MutableStateFlow(LlmProviderChoice.GEMINI_NANO)
    val selectedProvider: StateFlow<LlmProviderChoice> = _selectedProvider.asStateFlow()

    // Engine — rebuilt when provider changes
    private var _engine: HalogenEngine = buildEngine(LlmProviderChoice.GEMINI_NANO)
    val engine: HalogenEngine get() = _engine

    // Nano status
    private val _nanoStatus = MutableStateFlow(HalogenLlmAvailability.UNAVAILABLE)
    val nanoStatus: StateFlow<HalogenLlmAvailability> = _nanoStatus.asStateFlow()

    // Dark mode override (null = follow system)
    private val _darkOverride = MutableStateFlow<Boolean?>(null)
    val darkOverride: StateFlow<Boolean?> = _darkOverride.asStateFlow()

    init {
        // Start nano polling
        viewModelScope.launch {
            while (_nanoStatus.value != HalogenLlmAvailability.READY) {
                try {
                    _nanoStatus.value = nanoProvider.availability()
                } catch (_: Exception) {
                }
                delay(3000)
            }
        }

        // Rebuild engine when provider changes
        viewModelScope.launch {
            _selectedProvider.collect { choice ->
                _engine = buildEngine(choice)
            }
        }
    }

    fun setSelectedProvider(choice: LlmProviderChoice) {
        _selectedProvider.value = choice
    }

    fun toggleDarkMode(currentIsDark: Boolean) {
        _darkOverride.value = !currentIsDark
    }

    private fun buildEngine(choice: LlmProviderChoice): HalogenEngine {
        Log.d("HalogenDemo", "Building engine with provider: ${choice.label}")
        val builder = Halogen.Builder()
            .defaultTheme(HalogenDefaults.materialYou())
            .cache(HalogenCache.memory())
        when (choice) {
            LlmProviderChoice.GEMINI_NANO -> builder.provider(nanoProvider)
            LlmProviderChoice.OPENAI -> builder.provider(OpenAiProvider(openAiKey))
        }
        return builder.build()
    }
}
