package me.mmckenna.halogen.sample.llms.openai

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double,
    @SerializedName("max_tokens") val maxTokens: Int,
)

data class Message(val role: String, val content: String)

data class ChatResponse(val choices: List<Choice>)
data class Choice(val message: Message)
