package me.mmckenna.halogen.sample.screens.testharness

import halogen.HalogenConfig
import halogen.HalogenThemeSpec
import halogen.ThemeExpander
import halogen.engine.HalogenEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal val testPrompts = listOf(
    "sun",
    "ocean",
    "forest",
    "cyberpunk",
    "coffee shop",
    "lavender field",
    "desert sand",
    "arctic ice",
    "cherry blossom",
    "thunderstorm",
)

/**
 * Print the per-config matrix where each prompt x config has its own LLM-generated spec.
 */
internal fun printPerConfigMatrix(
    specMap: Map<String, HalogenThemeSpec>,
    prompts: List<String>,
    configNames: List<String>,
) {
    println("=== HALOGEN PER-CONFIG MATRIX ===")
    println("Configs: ${configNames.joinToString(", ")}")
    println()

    for (prompt in prompts) {
        println("--- $prompt ---")
        for (configName in configNames) {
            val cellKey = "$prompt:$configName"
            val spec = specMap[cellKey]
            if (spec != null) {
                val config = HalogenConfig.presets[configName] ?: HalogenConfig.Default
                val light = ThemeExpander.expandColors(spec, isDark = false, config = config)
                val dark = ThemeExpander.expandColors(spec, isDark = true, config = config)
                println("  [$configName] seeds: pri=${spec.primary} sec=${spec.secondary} ter=${spec.tertiary}")
                println("  [$configName] LIGHT: pri=${ThemeExpander.argbToHex(light.primary)} priCont=${ThemeExpander.argbToHex(light.primaryContainer)} sec=${ThemeExpander.argbToHex(light.secondary)} ter=${ThemeExpander.argbToHex(light.tertiary)} surf=${ThemeExpander.argbToHex(light.surface)}")
                println("  [$configName] DARK:  pri=${ThemeExpander.argbToHex(dark.primary)} priCont=${ThemeExpander.argbToHex(dark.primaryContainer)} sec=${ThemeExpander.argbToHex(dark.secondary)} ter=${ThemeExpander.argbToHex(dark.tertiary)} surf=${ThemeExpander.argbToHex(dark.surface)}")
            } else {
                println("  [$configName] FAILED")
            }
        }
        println()
    }
    println("=== END PER-CONFIG MATRIX ===")
}

data class TestHarnessUiState(
    val specMap: Map<String, HalogenThemeSpec> = emptyMap(),
    val enabledConfigs: Map<String, Boolean> = emptyMap(),
    val selectedPrompt: String = "",
    val isRunning: Boolean = false,
    val progressCurrent: Int = 0,
    val progressTotal: Int = 0,
    val statusMessage: String = "Ready",
    val errors: Map<String, String> = emptyMap(),
)

class TestHarnessState(
    private val engine: HalogenEngine,
    private val scope: CoroutineScope,
) {
    val allConfigNames: List<String> = HalogenConfig.presets.keys.toList()

    private val _uiState = MutableStateFlow(
        TestHarnessUiState(
            enabledConfigs = allConfigNames.associateWith { true },
            selectedPrompt = testPrompts.first(),
        ),
    )
    val uiState: StateFlow<TestHarnessUiState> = _uiState.asStateFlow()

    fun selectPrompt(prompt: String) {
        _uiState.update { it.copy(selectedPrompt = prompt) }
    }

    fun toggleConfig(name: String) {
        _uiState.update { state ->
            val current = state.enabledConfigs[name] ?: true
            state.copy(enabledConfigs = state.enabledConfigs + (name to !current))
        }
    }

    fun runAll() {
        val state = _uiState.value
        val activeConfigNames = allConfigNames.filter { state.enabledConfigs[it] == true }
        val totalCalls = testPrompts.size * activeConfigNames.size

        _uiState.update {
            it.copy(
                isRunning = true,
                errors = emptyMap(),
                specMap = emptyMap(),
                progressTotal = totalCalls,
                progressCurrent = 0,
            )
        }

        scope.launch {
            val newSpecMap = mutableMapOf<String, HalogenThemeSpec>()
            val newErrors = mutableMapOf<String, String>()
            var current = 0

            for (prompt in testPrompts) {
                for (configName in activeConfigNames) {
                    val config = HalogenConfig.presets[configName] ?: HalogenConfig.Default
                    engine.config = config
                    val cellKey = "$prompt:$configName"
                    current++
                    _uiState.update {
                        it.copy(
                            statusMessage = "$configName: $prompt ($current/$totalCalls)",
                            progressCurrent = current,
                        )
                    }
                    try {
                        engine.clearCache()
                        val result = engine.resolve(key = "harness:$cellKey", hint = prompt)
                        val spec = result.themeSpec
                        if (spec != null) {
                            newSpecMap[cellKey] = spec
                        } else {
                            newErrors[cellKey] = "No spec: ${result::class.simpleName}"
                        }
                    } catch (e: Exception) {
                        println("TestHarness: Error generating '$cellKey': ${e.message}")
                        newErrors[cellKey] = e.message ?: "Unknown error"
                    }
                    // Update state after each cell so UI shows progress
                    _uiState.update {
                        it.copy(specMap = newSpecMap.toMap(), errors = newErrors.toMap())
                    }
                }
            }

            val succeeded = newSpecMap.size
            _uiState.update {
                it.copy(
                    statusMessage = "Done ($succeeded/$totalCalls succeeded)",
                    isRunning = false,
                )
            }
            printPerConfigMatrix(newSpecMap, testPrompts, activeConfigNames)
        }
    }

    fun runSelected() {
        val state = _uiState.value
        val activeConfigNames = allConfigNames.filter { state.enabledConfigs[it] == true }

        _uiState.update {
            it.copy(
                isRunning = true,
                errors = emptyMap(),
                progressTotal = activeConfigNames.size,
                progressCurrent = 0,
            )
        }

        scope.launch {
            val newSpecMap = state.specMap.toMutableMap()
            val newErrors = mutableMapOf<String, String>()
            var current = 0

            for (configName in activeConfigNames) {
                val config = HalogenConfig.presets[configName] ?: HalogenConfig.Default
                engine.config = config
                val cellKey = "${state.selectedPrompt}:$configName"
                current++
                _uiState.update {
                    it.copy(
                        statusMessage = "$configName: ${state.selectedPrompt} ($current/${activeConfigNames.size})",
                        progressCurrent = current,
                    )
                }
                try {
                    engine.clearCache()
                    val result = engine.resolve(key = "harness:$cellKey", hint = state.selectedPrompt)
                    val spec = result.themeSpec
                    if (spec != null) {
                        newSpecMap[cellKey] = spec
                    } else {
                        newErrors[cellKey] = "No spec: ${result::class.simpleName}"
                    }
                } catch (e: Exception) {
                    println("TestHarness: Error generating '$cellKey': ${e.message}")
                    newErrors[cellKey] = e.message ?: "Unknown error"
                }
                // Update state after each cell so UI shows progress
                _uiState.update {
                    it.copy(specMap = newSpecMap.toMap(), errors = newErrors.toMap())
                }
            }

            _uiState.update {
                it.copy(
                    statusMessage = "Done",
                    isRunning = false,
                )
            }
            printPerConfigMatrix(newSpecMap, listOf(state.selectedPrompt), activeConfigNames)
        }
    }

    fun clearResults() {
        _uiState.update {
            it.copy(
                specMap = emptyMap(),
                errors = emptyMap(),
                progressCurrent = 0,
                progressTotal = 0,
                statusMessage = "Ready",
            )
        }
    }
}
