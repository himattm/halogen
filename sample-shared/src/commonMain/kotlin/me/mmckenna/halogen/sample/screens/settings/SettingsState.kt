package me.mmckenna.halogen.sample.screens.settings

import halogen.engine.HalogenEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = false,
)

class SettingsState(
    private val engine: HalogenEngine,
    private val scope: CoroutineScope,
) {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun generateGlobalTheme(prompt: String) {
        if (prompt.isBlank()) {
            engine.applyDefault()
        } else {
            _uiState.update { it.copy(isLoading = true) }
            scope.launch {
                engine.resolve(key = "global-$prompt", hint = prompt)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun clearCache() {
        scope.launch {
            engine.clearCache()
        }
    }

    fun resetToDefault() {
        engine.applyDefault()
    }

    fun clearCacheAndReset() {
        scope.launch {
            engine.clearCache()
            engine.applyDefault()
        }
    }
}
