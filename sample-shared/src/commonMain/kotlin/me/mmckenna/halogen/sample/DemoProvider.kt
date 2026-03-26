package me.mmckenna.halogen.sample

import halogen.HalogenLlmAvailability
import halogen.HalogenLlmProvider

/**
 * Offline demo provider that returns preset theme specs.
 *
 * Cycles through a set of curated themes so the demo works
 * without any network or on-device LLM.
 */
internal class DemoProvider : HalogenLlmProvider {

    private val presets = listOf(
        // Ocean
        """{"pri":"#356A8A","sec":"#5C8A9E","ter":"#7AACB5","neuL":"#F0F5F7","neuD":"#0E1B26","err":"#BA1A1A","font":"modern","hw":500,"bw":400,"ls":false,"cs":"soft","cx":1.2}""",
        // Neon
        """{"pri":"#9A6ACD","sec":"#4A8A8A","ter":"#B06B7D","neuL":"#F3F0F6","neuD":"#151018","err":"#93000A","font":"mono","hw":700,"bw":400,"ls":true,"cs":"sharp","cx":0.5}""",
        // Forest
        """{"pri":"#2E7D32","sec":"#558B2F","ter":"#795548","neuL":"#F1F8E9","neuD":"#1B2513","err":"#C62828","font":"classic","hw":600,"bw":400,"ls":false,"cs":"rounded","cx":1.0}""",
        // Sunset
        """{"pri":"#E65100","sec":"#FF8F00","ter":"#AD1457","neuL":"#FFF3E0","neuD":"#1A0E00","err":"#B71C1C","font":"modern","hw":600,"bw":300,"ls":false,"cs":"pill","cx":1.5}""",
        // Minimal
        """{"pri":"#37474F","sec":"#546E7A","ter":"#78909C","neuL":"#FAFAFA","neuD":"#121212","err":"#D32F2F","font":"minimal","hw":500,"bw":300,"ls":true,"cs":"sharp","cx":0.7}""",
    )

    private var index = 0

    override suspend fun generate(prompt: String): String {
        val spec = presets[index % presets.size]
        index++
        return spec
    }

    override suspend fun availability(): HalogenLlmAvailability =
        HalogenLlmAvailability.READY
}
