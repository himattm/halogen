package halogen

import kotlin.test.Test
import kotlin.test.assertTrue

class PromptBuilderTest {

    @Test
    fun build_containsSystemPrompt() {
        val prompt = PromptBuilder.build("ocean vibes")
        assertTrue(prompt.contains("Material Design 3 theme designer"), "Should contain system prompt")
    }

    @Test
    fun build_containsJsonFormatInstructions() {
        val prompt = PromptBuilder.build("ocean vibes")
        assertTrue(prompt.contains("\"pri\": \"#RRGGBB\""), "Should contain pri format")
        assertTrue(prompt.contains("\"sec\": \"#RRGGBB\""), "Should contain sec format")
    }

    @Test
    fun build_containsAllThreeFewShotExamples() {
        val prompt = PromptBuilder.build("anything")
        assertTrue(prompt.contains("ocean vibes, calming"), "Should contain first few-shot example")
        assertTrue(prompt.contains("neon cyberpunk"), "Should contain second few-shot example")
        assertTrue(prompt.contains("warm and cozy, rounded"), "Should contain third few-shot example")
    }

    @Test
    fun build_containsFewShotOutputs() {
        val prompt = PromptBuilder.build("anything")
        assertTrue(prompt.contains("#356A8A"), "Should contain first example's primary color")
        assertTrue(prompt.contains("#9A6ACD"), "Should contain second example's primary color")
        assertTrue(prompt.contains("#8B5E3C"), "Should contain third example's primary color")
    }

    @Test
    fun build_containsUserHint() {
        val prompt = PromptBuilder.build("dark mode cyberpunk neon")
        assertTrue(prompt.contains("dark mode cyberpunk neon"), "Should contain user hint")
    }

    @Test
    fun build_userHintAppearsAfterFewShot() {
        val prompt = PromptBuilder.build("my custom theme")
        val lastFewShotIndex = prompt.lastIndexOf("warm and cozy, rounded")
        val userHintIndex = prompt.indexOf("User: my custom theme")
        assertTrue(
            userHintIndex > lastFewShotIndex,
            "User hint should appear after the last few-shot example",
        )
    }

    @Test
    fun build_withoutExtensions_noExtensionBlock() {
        val prompt = PromptBuilder.build("simple theme")
        assertTrue(
            !prompt.contains("Also include these custom color tokens"),
            "Should not contain extension instructions when no extensions provided",
        )
    }

    @Test
    fun build_withExtensions_appendsExtensionInstructions() {
        val extensions = listOf(
            HalogenExtension("success", "A green color for success states"),
            HalogenExtension("warning", "An amber color for warnings"),
        )
        val prompt = PromptBuilder.build("ocean vibes", extensions)
        assertTrue(
            prompt.contains("Also include these custom color tokens"),
            "Should contain extension instructions",
        )
        assertTrue(
            prompt.contains("\"success\": A green color for success states"),
            "Should contain success extension",
        )
        assertTrue(
            prompt.contains("\"warning\": An amber color for warnings"),
            "Should contain warning extension",
        )
    }

    @Test
    fun build_extensionsAppearBeforeUserHint() {
        val extensions = listOf(
            HalogenExtension("success", "A green color"),
        )
        val prompt = PromptBuilder.build("my theme", extensions)
        val extIndex = prompt.indexOf("Also include these custom color tokens")
        val userIndex = prompt.indexOf("User: my theme")
        assertTrue(
            extIndex < userIndex,
            "Extension block should appear before the user hint",
        )
    }

    @Test
    fun build_containsRules() {
        val prompt = PromptBuilder.build("anything")
        assertTrue(prompt.contains("Rules:"), "Should contain rules section")
        assertTrue(prompt.contains("Output ONLY the JSON object"), "Should contain output rule")
    }

    // --- Custom Prompt Instructions tests ---

    @Test
    fun build_withAdditionalInstructions_containsRulesSection() {
        val prompt = PromptBuilder.build(
            "ocean",
            additionalInstructions = "Always use brand blue #1A73E8 as primary",
        )
        assertTrue(prompt.contains("Additional rules:"), "Should contain additional rules header")
        assertTrue(prompt.contains("Always use brand blue #1A73E8 as primary"), "Should contain instruction text")
    }

    @Test
    fun build_additionalInstructions_afterConfigGuidance_beforeBuiltInExamples() {
        val config = HalogenConfig(promptGuidance = "Vibrant style guidance")
        val prompt = PromptBuilder.build(
            "ocean",
            config = config,
            additionalInstructions = "Prefer blues and greens",
        )
        val guidanceIndex = prompt.indexOf("Vibrant style guidance")
        val instructionIndex = prompt.indexOf("Prefer blues and greens")
        val firstBuiltInIndex = prompt.indexOf("ocean vibes, calming")
        assertTrue(guidanceIndex < instructionIndex, "Config guidance should come before instructions")
        assertTrue(instructionIndex < firstBuiltInIndex, "Instructions should come before built-in examples")
    }

    @Test
    fun build_withCustomExamples_afterBuiltInExamples() {
        val prompt = PromptBuilder.build(
            "ocean",
            customExamples = listOf("quarterly earnings" to """{"pri":"#1A73E8"}"""),
        )
        val lastBuiltInIndex = prompt.lastIndexOf("warm and cozy, rounded")
        val customExampleIndex = prompt.indexOf("quarterly earnings")
        assertTrue(customExampleIndex > lastBuiltInIndex, "Custom examples should appear after built-in examples")
    }

    @Test
    fun build_customExamples_beforeExtensions() {
        val extensions = listOf(HalogenExtension("success", "A green color"))
        val prompt = PromptBuilder.build(
            "ocean",
            extensions = extensions,
            customExamples = listOf("finance" to """{"pri":"#2E5C8A"}"""),
        )
        val customIndex = prompt.indexOf("finance")
        val extIndex = prompt.indexOf("Also include these custom color tokens")
        assertTrue(customIndex < extIndex, "Custom examples should appear before extensions")
    }

    @Test
    fun build_fullOrdering_system_guidance_instructions_builtIn_custom_extensions_hint() {
        val config = HalogenConfig(promptGuidance = "Vibrant style", styleName = "vibrant")
        val extensions = listOf(HalogenExtension("brand", "Brand accent color"))
        val prompt = PromptBuilder.build(
            "my theme",
            extensions = extensions,
            config = config,
            additionalInstructions = "Never use playful fonts",
            customExamples = listOf("quarterly" to """{"pri":"#1A73E8"}"""),
        )
        val systemIdx = prompt.indexOf("Material Design 3 theme designer")
        val guidanceIdx = prompt.indexOf("Vibrant style")
        val instructionIdx = prompt.indexOf("Never use playful fonts")
        val builtInIdx = prompt.indexOf("ocean vibes, calming")
        val customIdx = prompt.indexOf("quarterly")
        val extIdx = prompt.indexOf("Also include these custom color tokens")
        val hintIdx = prompt.lastIndexOf("User: vibrant my theme")

        assertTrue(systemIdx < guidanceIdx, "System before guidance")
        assertTrue(guidanceIdx < instructionIdx, "Guidance before instructions")
        assertTrue(instructionIdx < builtInIdx, "Instructions before built-in examples")
        assertTrue(builtInIdx < customIdx, "Built-in before custom examples")
        assertTrue(customIdx < extIdx, "Custom examples before extensions")
        assertTrue(extIdx < hintIdx, "Extensions before user hint")
    }

    @Test
    fun build_withBothInstructionsAndExamples_containsBoth() {
        val prompt = PromptBuilder.build(
            "ocean",
            additionalInstructions = "Finance app constraints",
            customExamples = listOf("budget" to """{"pri":"#2E5C8A"}"""),
        )
        assertTrue(prompt.contains("Finance app constraints"), "Should contain instructions")
        assertTrue(prompt.contains("budget"), "Should contain custom example input")
        assertTrue(prompt.contains("#2E5C8A"), "Should contain custom example output")
    }

    // --- estimateTokenCount tests ---

    @Test
    fun estimateTokenCount_emptyString_returnsZero() {
        val count = PromptBuilder.estimateTokenCount("")
        assertTrue(count == 0, "Empty string should estimate 0 tokens, got $count")
    }

    @Test
    fun estimateTokenCount_fourChars_returnsOne() {
        val count = PromptBuilder.estimateTokenCount("abcd")
        assertTrue(count == 1, "4 chars should estimate 1 token, got $count")
    }

    @Test
    fun estimateTokenCount_fiveChars_returnsTwo() {
        val count = PromptBuilder.estimateTokenCount("abcde")
        assertTrue(count == 2, "5 chars should estimate 2 tokens (ceiling), got $count")
    }

    // --- tokenBreakdown tests ---

    @Test
    fun tokenBreakdown_default_containsSystemPromptLine() {
        val breakdown = PromptBuilder.tokenBreakdown()
        assertTrue(breakdown.contains("System prompt:"), "Should contain system prompt line")
        assertTrue(breakdown.contains("Built-in examples:"), "Should contain built-in examples line")
    }

    @Test
    fun tokenBreakdown_withInstructions_containsInstructionsLine() {
        val breakdown = PromptBuilder.tokenBreakdown(
            additionalInstructions = "Always use brand blue",
        )
        assertTrue(breakdown.contains("Instructions:"), "Should contain instructions line")
    }

    @Test
    fun tokenBreakdown_withCustomExamples_containsCustomExamplesLine() {
        val breakdown = PromptBuilder.tokenBreakdown(
            customExamples = listOf("test" to """{"pri":"#FF0000"}"""),
        )
        assertTrue(breakdown.contains("Custom examples:"), "Should contain custom examples line")
    }

    @Test
    fun tokenBreakdown_withExtensions_containsExtensionsLine() {
        val breakdown = PromptBuilder.tokenBreakdown(
            extensions = listOf(
                HalogenExtension("success", "Green color"),
                HalogenExtension("warning", "Amber color"),
            ),
        )
        assertTrue(breakdown.contains("Extensions (2):"), "Should contain extensions line with count")
    }
}
