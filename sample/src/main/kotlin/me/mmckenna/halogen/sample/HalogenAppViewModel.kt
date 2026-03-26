package me.mmckenna.halogen.sample

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import halogen.HalogenDefaults
import halogen.HalogenLlmAvailability
import halogen.cache.room.HalogenRoomCache
import halogen.engine.Halogen
import halogen.engine.HalogenEngine
import halogen.provider.nano.GeminiNanoProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HalogenAppViewModel(application: Application) : AndroidViewModel(application) {
    val nanoProvider = GeminiNanoProvider()

    val engine: HalogenEngine = Halogen.Builder()
        .provider(nanoProvider)
        .defaultTheme(HalogenDefaults.materialYou())
        .cache(HalogenRoomCache.create())
        .build()

    private val _nanoStatus = MutableStateFlow(HalogenLlmAvailability.UNAVAILABLE)
    val nanoStatus: StateFlow<HalogenLlmAvailability> = _nanoStatus.asStateFlow()

    private val _darkOverride = MutableStateFlow<Boolean?>(null)
    val darkOverride: StateFlow<Boolean?> = _darkOverride.asStateFlow()

    init {
        viewModelScope.launch {
            while (_nanoStatus.value != HalogenLlmAvailability.READY) {
                try {
                    _nanoStatus.value = nanoProvider.availability()
                } catch (_: Exception) {
                }
                delay(3000)
            }
            Log.d("HalogenDemo", "Gemini Nano is ready")
        }
    }

    fun toggleDarkMode(currentIsDark: Boolean) {
        _darkOverride.value = !currentIsDark
    }

    fun updateTuningParams(
        temperature: Float,
        topK: Int,
        topP: Float?,
    ) {
        nanoProvider.temperature = temperature
        nanoProvider.topK = topK
        nanoProvider.topP = topP
    }
}
