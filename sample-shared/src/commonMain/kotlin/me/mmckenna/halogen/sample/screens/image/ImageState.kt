package me.mmckenna.halogen.sample.screens.image

import coil3.ImageLoader
import coil3.PlatformContext
import halogen.HalogenThemeSpec
import halogen.engine.HalogenEngine
import halogen.engine.HalogenResult
import halogen.image.DominantColors
import halogen.image.extractColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImageUiState(
    val selectedPreset: PresetImage? = null,
    val dominantColors: DominantColors? = null,
    val themeSpec: HalogenThemeSpec? = null,
    val isLoading: Boolean = false,
    val useLlm: Boolean = false,
    val error: String? = null,
)

class ImageState(
    private val engine: HalogenEngine,
    private val scope: CoroutineScope,
) {
    private val _uiState = MutableStateFlow(ImageUiState())
    val uiState: StateFlow<ImageUiState> = _uiState.asStateFlow()

    fun selectPreset(
        preset: PresetImage,
        imageLoader: ImageLoader,
        context: PlatformContext,
    ) {
        _uiState.update { it.copy(selectedPreset = preset, isLoading = true, error = null) }

        scope.launch {
            val colors = extractColors(preset.url, imageLoader, context)
            if (colors == null || colors.colors.isEmpty()) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Could not extract colors from image")
                }
                return@launch
            }

            _uiState.update { it.copy(dominantColors = colors) }
            applyTheme(preset, colors)
        }
    }

    fun toggleLlm(imageLoader: ImageLoader, context: PlatformContext) {
        val current = _uiState.value
        val newUseLlm = !current.useLlm
        _uiState.update { it.copy(useLlm = newUseLlm) }

        val preset = current.selectedPreset ?: return
        val colors = current.dominantColors ?: return

        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            applyTheme(preset, colors)
        }
    }

    private suspend fun applyTheme(preset: PresetImage, colors: DominantColors) {
        val useLlm = _uiState.value.useLlm
        val key = "image:${preset.label}:${if (useLlm) "llm" else "algo"}"

        try {
            if (useLlm) {
                val result = engine.resolve(key, colors.toHint())
                when (result) {
                    is HalogenResult.Success, is HalogenResult.Cached -> {
                        _uiState.update {
                            it.copy(themeSpec = result.themeSpec, isLoading = false)
                        }
                    }
                    else -> {
                        val spec = colors.toSpec()
                        engine.apply(key, spec)
                        _uiState.update {
                            it.copy(
                                themeSpec = spec,
                                isLoading = false,
                                error = "LLM unavailable — using algorithmic",
                            )
                        }
                    }
                }
            } else {
                val spec = colors.toSpec()
                engine.apply(key, spec)
                _uiState.update { it.copy(themeSpec = spec, isLoading = false) }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = e.message) }
        }
    }
}
