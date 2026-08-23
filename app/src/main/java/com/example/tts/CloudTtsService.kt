package com.example.tts

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

data class CloudTtsRequest(
    val model: String = "tts-1",
    val input: String,
    val voice: String = "alloy",
    val response_format: String = "mp3",
    val speed: Double = 1.0
)

interface CloudTtsService {
    @POST
    suspend fun synthesize(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: CloudTtsRequest
    ): Response<ResponseBody>
}
