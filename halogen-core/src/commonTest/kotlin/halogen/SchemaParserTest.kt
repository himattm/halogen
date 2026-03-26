package halogen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SchemaParserTest {

    private val validJson = """
        {
            "pri": "#6750A4",
            "sec": "#625B71",
            "ter": "#7D5260",
            "neuL": "#FFFBFE",
            "neuD": "#1C1B1F",
            "err": "#B3261E",
            "font": "modern",
            "hw": 700,
            "bw": 400,
            "ls": false,
            "cs": "rounded",
            "cx": 1.0
        }
    """.trimIndent()

    // ---- Valid JSON ----

    @Test
    fun parse_validJson_succeeds() {
        val result = SchemaParser.parse(validJson)
        assertTrue(result.isSuccess, "Should successfully parse valid JSON")
        val spec = result.getOrThrow()
        assertEquals("#6750A4", spec.primary)
        assertEquals("#625B71", spec.secondary)
        assertEquals("#7D5260", spec.tertiary)
        assertEquals("#FFFBFE", spec.neutralLight)
        assertEquals("#1C1B1F", spec.neutralDark)
        assertEquals("#B3261E", spec.error)
        assertEquals("modern", spec.fontMood)
        assertEquals(700, spec.headingWeight)
        assertEquals(400, spec.bodyWeight)
        assertEquals(false, spec.tightLetterSpacing)
        assertEquals("rounded", spec.cornerStyle)
        assertEquals(1.0f, spec.cornerScale)
    }

    // ---- Markdown fences ----

    @Test
    fun parse_withJsonCodeFences_succeeds() {
        val wrapped = "```json\n$validJson\n```"
        val result = SchemaParser.parse(wrapped)
        assertTrue(result.isSuccess, "Should parse JSON wrapped in ```json fences")
        assertEquals("#6750A4", result.getOrThrow().primary)
    }

    @Test
    fun parse_withPlainCodeFences_succeeds() {
        val wrapped = "```\n$validJson\n```"
        val result = SchemaParser.parse(wrapped)
        assertTrue(result.isSuccess, "Should parse JSON wrapped in plain ``` fences")
        assertEquals("#6750A4", result.getOrThrow().primary)
    }

    // ---- Invalid JSON ----

    @Test
    fun parse_invalidJson_fails() {
        val result = SchemaParser.parse("not json at all")
        assertTrue(result.isFailure, "Should fail on invalid JSON")
        assertTrue(
            result.exceptionOrNull()?.message?.contains("Failed to parse") == true,
            "Error message should mention parse failure",
        )
    }

    @Test
    fun parse_emptyString_fails() {
        val result = SchemaParser.parse("")
        assertTrue(result.isFailure, "Should fail on empty string")
    }

    // ---- Missing fields ----

    @Test
    fun parse_missingRequiredField_fails() {
        val incomplete = """
            {
                "pri": "#6750A4",
                "sec": "#625B71"
            }
        """.trimIndent()
        val result = SchemaParser.parse(incomplete)
        assertTrue(result.isFailure, "Should fail when required fields are missing")
    }

    // ---- Extra fields ----

    @Test
    fun parse_extraFields_succeeds() {
        val withExtra = """
            {
                "pri": "#6750A4",
                "sec": "#625B71",
                "ter": "#7D5260",
                "neuL": "#FFFBFE",
                "neuD": "#1C1B1F",
                "err": "#B3261E",
                "font": "modern",
                "hw": 700,
                "bw": 400,
                "ls": false,
                "cs": "rounded",
                "cx": 1.0,
                "unknownField": "should be ignored"
            }
        """.trimIndent()
        val result = SchemaParser.parse(withExtra)
        assertTrue(result.isSuccess, "Should succeed when extra/unknown fields are present")
    }

    // ---- Validation: hex colors ----

    @Test
    fun parse_invalidHexColor_fails() {
        val badColor = validJson.replace("#6750A4", "#ZZZZZZ")
        val result = SchemaParser.parse(badColor)
        assertTrue(result.isFailure, "Should fail on invalid hex color")
        assertTrue(
            result.exceptionOrNull()?.message?.contains("Invalid hex color") == true,
            "Error should mention invalid hex color",
        )
    }

    @Test
    fun parse_shortHexColor_fails() {
        val badColor = validJson.replace("#6750A4", "#675")
        val result = SchemaParser.parse(badColor)
        assertTrue(result.isFailure, "Should fail on short hex color")
    }

    // ---- Clamping: weights ----

    @Test
    fun parse_clampsHeadingWeight() {
        val overWeight = validJson.replace("\"hw\": 700", "\"hw\": 1200")
        val result = SchemaParser.parse(overWeight)
        assertTrue(result.isSuccess)
        assertEquals(900, result.getOrThrow().headingWeight, "Heading weight should clamp to 900")
    }

    @Test
    fun parse_clampsBodyWeight() {
        val underWeight = validJson.replace("\"bw\": 400", "\"bw\": 0")
        val result = SchemaParser.parse(underWeight)
        assertTrue(result.isSuccess)
        assertEquals(100, result.getOrThrow().bodyWeight, "Body weight should clamp to 100")
    }

    // ---- Clamping: corner scale ----

    @Test
    fun parse_clampsCornerScale_tooHigh() {
        val highScale = validJson.replace("\"cx\": 1.0", "\"cx\": 5.0")
        val result = SchemaParser.parse(highScale)
        assertTrue(result.isSuccess)
        assertEquals(2.0f, result.getOrThrow().cornerScale, "Corner scale should clamp to 2.0")
    }

    @Test
    fun parse_clampsCornerScale_negative() {
        val negScale = validJson.replace("\"cx\": 1.0", "\"cx\": -1.0")
        val result = SchemaParser.parse(negScale)
        assertTrue(result.isSuccess)
        assertEquals(0.0f, result.getOrThrow().cornerScale, "Corner scale should clamp to 0.0")
    }

    // ---- Extensions ----

    @Test
    fun parse_withExtensions_succeeds() {
        val withExt = """
            {
                "pri": "#6750A4",
                "sec": "#625B71",
                "ter": "#7D5260",
                "neuL": "#FFFBFE",
                "neuD": "#1C1B1F",
                "err": "#B3261E",
                "font": "modern",
                "hw": 700,
                "bw": 400,
                "ls": false,
                "cs": "rounded",
                "cx": 1.0,
                "ext": {"success": "#4CAF50", "warning": "#FF9800"}
            }
        """.trimIndent()
        val result = SchemaParser.parse(withExt)
        assertTrue(result.isSuccess)
        val ext = result.getOrThrow().extensions
        assertEquals("#4CAF50", ext?.get("success"))
        assertEquals("#FF9800", ext?.get("warning"))
    }
}
