package me.mmckenna.halogen.sample.llms.openai

import retrofit2.http.Body
import retrofit2.http.POST

interface OpenAiService {
    @POST("v1/chat/completions")
    suspend fun chatCompletions(@Body request: ChatRequest): ChatResponse
}
