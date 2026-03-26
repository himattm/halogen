package me.mmckenna.halogen.sample.llms.openai

import halogen.HalogenLlmAvailability
import halogen.HalogenLlmException
import halogen.HalogenLlmProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.mmckenna.halogen.sample.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * OpenAI provider for Halogen using Retrofit + OkHttp.
 */
class OpenAiProvider(
    private val apiKey: String,
    private val model: String = "gpt-4o-mini",
    private val temperature: Double = 0.3,
) : HalogenLlmProvider {

    private val service: OpenAiService by lazy {
        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $apiKey")
                .build()
            chain.proceed(request)
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenAiService::class.java)
    }

    override suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val request = ChatRequest(
                model = model,
                messages = listOf(Message(role = "user", content = prompt)),
                temperature = temperature,
                maxTokens = 300,
            )
            val response = service.chatCompletions(request)
            response.choices.first().message.content
        } catch (e: HttpException) {
            val code = e.code()
            val errorBody = e.response()?.errorBody()?.string() ?: "unknown error"
            throw HalogenLlmException(
                "OpenAI API error $code: $errorBody",
                isRetryable = code >= 500,
            )
        }
    }

    override suspend fun availability(): HalogenLlmAvailability {
        return if (apiKey.isNotBlank()) {
            HalogenLlmAvailability.READY
        } else {
            HalogenLlmAvailability.UNAVAILABLE
        }
    }
}
