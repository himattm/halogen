package me.mmckenna.halogen.sample.llms.openai

import halogen.HalogenLlmAvailability
import halogen.HalogenLlmException
import halogen.HalogenLlmProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class OpenAiProvider(
    private val apiKey: String,
    private val model: String = "gpt-4o-mini",
    var temperature: Float = 0.3f,
    var topP: Float? = null,
    var maxTokens: Int = 300,
) : HalogenLlmProvider {

    private val client by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    override suspend fun generate(prompt: String): String {
        val request = ChatRequest(
            model = model,
            messages = listOf(Message(role = "user", content = prompt)),
            temperature = temperature,
            maxTokens = maxTokens,
            topP = topP,
        )
        val response = try {
            client.post("https://api.openai.com/v1/chat/completions") {
                contentType(ContentType.Application.Json)
                bearerAuth(apiKey)
                setBody(request)
            }
        } catch (e: Exception) {
            throw HalogenLlmException("OpenAI request failed: ${e.message}", e, isRetryable = true)
        }
        if (!response.status.isSuccess()) {
            val body = response.bodyAsText()
            throw HalogenLlmException(
                "OpenAI API error ${response.status.value}: $body",
                isRetryable = response.status.value >= 500,
            )
        }
        val chatResponse = response.body<ChatResponse>()
        val choice = chatResponse.choices.firstOrNull()
            ?: throw HalogenLlmException("OpenAI returned no choices", isRetryable = false)
        return choice.message.content
    }

    override suspend fun availability(): HalogenLlmAvailability {
        return if (apiKey.isNotBlank()) {
            HalogenLlmAvailability.READY
        } else {
            HalogenLlmAvailability.UNAVAILABLE
        }
    }
}
