package com.example.network

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url
import retrofit2.Response

interface LlmApiService {

    @POST
    suspend fun sendAnthropicMessage(
        @Url url: String = "https://api.anthropic.com/v1/messages",
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        @Header("content-type") contentType: String = "application/json",
        @Body request: AnthropicRequest
    ): Response<AnthropicResponse>

    @POST
    suspend fun sendOpenAiMessage(
        @Url url: String = "https://api.openai.com/v1/chat/completions",
        @Header("Authorization") authorization: String,
        @Body request: OpenAiRequest
    ): Response<OpenAiResponse>

    @POST
    suspend fun sendGeminiMessage(
        @Url url: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
}
