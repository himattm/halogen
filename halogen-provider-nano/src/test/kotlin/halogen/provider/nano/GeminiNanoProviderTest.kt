package halogen.provider.nano

import com.google.common.truth.Truth.assertThat
import halogen.HalogenLlmProvider
import org.junit.Test

/**
 * Unit tests for [GeminiNanoProvider].
 *
 * These tests verify constructor defaults, parameter mutability, and interface
 * conformance. ML Kit-dependent behavior (generate, availability, downloadModel,
 * warmup, close) requires an on-device model and is tested via instrumented tests.
 */
class GeminiNanoProviderTest {

    // ---- Interface conformance ----

    @Test
    fun `implements HalogenLlmProvider`() {
        val provider = GeminiNanoProvider()
        assertThat(provider).isInstanceOf(HalogenLlmProvider::class.java)
    }

    // ---- Constructor defaults ----

    @Test
    fun `default temperature is 0_2`() {
        val provider = GeminiNanoProvider()
        assertThat(provider.temperature).isEqualTo(0.2f)
    }

    @Test
    fun `default topK is 10`() {
        val provider = GeminiNanoProvider()
        assertThat(provider.topK).isEqualTo(10)
    }

    @Test
    fun `default topP is null`() {
        val provider = GeminiNanoProvider()
        assertThat(provider.topP).isNull()
    }

    // ---- Custom constructor values ----

    @Test
    fun `custom temperature is stored`() {
        val provider = GeminiNanoProvider(temperature = 0.8f)
        assertThat(provider.temperature).isEqualTo(0.8f)
    }

    @Test
    fun `custom topK is stored`() {
        val provider = GeminiNanoProvider(topK = 40)
        assertThat(provider.topK).isEqualTo(40)
    }

    @Test
    fun `custom topP is stored`() {
        val provider = GeminiNanoProvider(topP = 0.95f)
        assertThat(provider.topP).isEqualTo(0.95f)
    }

    @Test
    fun `all custom parameters are stored together`() {
        val provider = GeminiNanoProvider(
            temperature = 0.5f,
            topK = 25,
            topP = 0.9f,
        )
        assertThat(provider.temperature).isEqualTo(0.5f)
        assertThat(provider.topK).isEqualTo(25)
        assertThat(provider.topP).isEqualTo(0.9f)
    }

    // ---- Mutable property setters ----

    @Test
    fun `temperature is mutable`() {
        val provider = GeminiNanoProvider()
        provider.temperature = 0.7f
        assertThat(provider.temperature).isEqualTo(0.7f)
    }

    @Test
    fun `topK is mutable`() {
        val provider = GeminiNanoProvider()
        provider.topK = 50
        assertThat(provider.topK).isEqualTo(50)
    }

    @Test
    fun `topP is mutable`() {
        val provider = GeminiNanoProvider()
        assertThat(provider.topP).isNull()
        provider.topP = 0.85f
        assertThat(provider.topP).isEqualTo(0.85f)
    }

    @Test
    fun `topP can be set back to null`() {
        val provider = GeminiNanoProvider(topP = 0.9f)
        assertThat(provider.topP).isEqualTo(0.9f)
        provider.topP = null
        assertThat(provider.topP).isNull()
    }

    // ---- Boundary values ----

    @Test
    fun `temperature zero is accepted`() {
        val provider = GeminiNanoProvider(temperature = 0.0f)
        assertThat(provider.temperature).isEqualTo(0.0f)
    }

    @Test
    fun `temperature one is accepted`() {
        val provider = GeminiNanoProvider(temperature = 1.0f)
        assertThat(provider.temperature).isEqualTo(1.0f)
    }

    @Test
    fun `topK of 1 is accepted`() {
        val provider = GeminiNanoProvider(topK = 1)
        assertThat(provider.topK).isEqualTo(1)
    }

    @Test
    fun `topP zero is accepted`() {
        val provider = GeminiNanoProvider(topP = 0.0f)
        assertThat(provider.topP).isEqualTo(0.0f)
    }

    @Test
    fun `topP one is accepted`() {
        val provider = GeminiNanoProvider(topP = 1.0f)
        assertThat(provider.topP).isEqualTo(1.0f)
    }

    // ---- Multiple mutations ----

    @Test
    fun `parameters can be changed multiple times`() {
        val provider = GeminiNanoProvider()

        provider.temperature = 0.1f
        provider.temperature = 0.9f
        assertThat(provider.temperature).isEqualTo(0.9f)

        provider.topK = 5
        provider.topK = 100
        assertThat(provider.topK).isEqualTo(100)

        provider.topP = 0.5f
        provider.topP = 0.99f
        assertThat(provider.topP).isEqualTo(0.99f)
    }

    // ---- Independent instances ----

    @Test
    fun `separate instances have independent state`() {
        val a = GeminiNanoProvider(temperature = 0.1f, topK = 5)
        val b = GeminiNanoProvider(temperature = 0.9f, topK = 50)

        assertThat(a.temperature).isEqualTo(0.1f)
        assertThat(b.temperature).isEqualTo(0.9f)
        assertThat(a.topK).isEqualTo(5)
        assertThat(b.topK).isEqualTo(50)

        // Mutating one does not affect the other
        a.temperature = 0.5f
        assertThat(a.temperature).isEqualTo(0.5f)
        assertThat(b.temperature).isEqualTo(0.9f)
    }
}
