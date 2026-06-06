package halogen.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HintExtractorTest {

    @Test
    fun extract_blankInput_returnsNull() {
        assertNull(HintExtractor.extract(""))
        assertNull(HintExtractor.extract("   "))
    }

    @Test
    fun extract_subredditPrefix_strips() {
        assertEquals("programming", HintExtractor.extract("/r/programming"))
    }

    @Test
    fun extract_camelCase_splits() {
        assertEquals("dark mode", HintExtractor.extract("darkMode"))
    }

    @Test
    fun extract_snakeCase_splits() {
        assertEquals("dark mode", HintExtractor.extract("dark_mode"))
    }

    @Test
    fun extract_kebabCase_splits() {
        assertEquals("dark mode", HintExtractor.extract("dark-mode"))
    }

    @Test
    fun extract_hexId_returnsNull() {
        assertNull(HintExtractor.extract("a1b2c3d4e5f6"))
    }

    @Test
    fun extract_pathTakesLastSegment() {
        assertEquals("programming", HintExtractor.extract("/category/tech/programming"))
    }

    @Test
    fun extract_topicPrefix_strips() {
        assertEquals("material you", HintExtractor.extract("/topic/material-you"))
    }

    @Test
    fun extract_hashPrefix_strips() {
        assertEquals("dark mode", HintExtractor.extract("#darkMode"))
    }

    @Test
    fun extract_repeatedSeparators_normalizesWhitespace() {
        assertEquals("dark mode", HintExtractor.extract(" dark__-- \t mode "))
    }

    @Test
    fun extract_numericOnly_returnsNull() {
        assertNull(HintExtractor.extract("123456"))
    }

    @Test
    fun extract_hexIdWithSeparators_returnsNull() {
        assertNull(HintExtractor.extract("a1b2-c3d4"))
    }
}
