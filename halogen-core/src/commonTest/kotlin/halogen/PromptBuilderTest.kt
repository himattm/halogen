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
}
