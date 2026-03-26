package me.mmckenna.halogen.sample.llms.openai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Float = 0.7f,
    @SerialName("max_tokens") val maxTokens: Int = 300,
    @SerialName("top_p") val topP: Float? = null,
)

@Serializable
internal data class Message(val role: String, val content: String)

@Serializable
internal data class ChatResponse(val choices: List<Choice>)

@Serializable
internal data class Choice(val message: Message)
