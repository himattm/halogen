package me.mmckenna.halogen.sample.screens.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import halogen.engine.HalogenEngine
import halogen.provider.nano.GeminiNanoProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadMessage: String? = null,
)

class SettingsViewModel(
    private val engine: HalogenEngine,
    private val nanoProvider: GeminiNanoProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun generateGlobalTheme(prompt: String) {
        if (prompt.isBlank()) {
            engine.applyDefault()
        } else {
            _uiState.update { it.copy(isLoading = true) }
            viewModelScope.launch {
                engine.resolve(key = "global-$prompt", hint = prompt)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun downloadModel() {
        _uiState.update { it.copy(isDownloading = true, downloadMessage = "Triggering model download...") }
        viewModelScope.launch {
            try {
                nanoProvider.downloadModel().collect { status ->
                    _uiState.update { it.copy(downloadMessage = "Download: $status") }
                    Log.d("HalogenDemo", "Download status: $status")
                }
                _uiState.update { it.copy(downloadMessage = "Download complete", isDownloading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(downloadMessage = "Download error: ${e.message}", isDownloading = false) }
                Log.e("HalogenDemo", "Download failed", e)
            }
        }
    }

    fun warmupModel() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(downloadMessage = "Warming up model...") }
                nanoProvider.warmup()
                _uiState.update { it.copy(downloadMessage = "Model warmed up") }
            } catch (e: Exception) {
                _uiState.update { it.copy(downloadMessage = "Warmup error: ${e.message}") }
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            engine.clearCache()
        }
    }

    fun resetToDefault() {
        engine.applyDefault()
    }

    fun clearCacheAndReset() {
        viewModelScope.launch {
            engine.clearCache()
            engine.applyDefault()
        }
    }

    companion object {
        fun factory(engine: HalogenEngine, nanoProvider: GeminiNanoProvider) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(engine, nanoProvider) as T
            }
        }
    }
}
