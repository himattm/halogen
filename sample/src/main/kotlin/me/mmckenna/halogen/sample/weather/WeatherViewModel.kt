package me.mmckenna.halogen.sample.weather

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import halogen.engine.HalogenEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeatherUiState(
    val selectedIndex: Int = 1,
    val isLoading: Boolean = false,
)

class WeatherViewModel(
    private val engine: HalogenEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    init {
        // Auto-generate when selectedIndex changes
        viewModelScope.launch {
            _uiState.map { it.selectedIndex }
                .distinctUntilChanged()
                .collect { index ->
                    generateForWeather(index)
                }
        }
    }

    fun selectWeather(index: Int) {
        _uiState.update { it.copy(selectedIndex = index) }
    }

    private suspend fun generateForWeather(index: Int) {
        val weather = weatherConditions[index]
        _uiState.update { it.copy(isLoading = true) }
        Log.d("Weather", "Generating theme for: ${weather.name}")
        engine.resolve(key = "weather:${weather.name}", hint = weather.hint)
        _uiState.update { it.copy(isLoading = false) }
    }

    companion object {
        fun factory(engine: HalogenEngine) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return WeatherViewModel(engine) as T
            }
        }
    }
}
