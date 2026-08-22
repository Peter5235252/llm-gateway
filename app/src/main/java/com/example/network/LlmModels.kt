package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// --- Anthropic Models ---
@JsonClass(generateAdapter = true)
data class AnthropicRequest(
    val model: String = "claude-sonnet-5",
    @Json(name = "max_tokens") val maxTokens: Int = 1024,
    val messages: List<AnthropicMessage>
)

@JsonClass(generateAdapter = true)
data class AnthropicMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class AnthropicResponse(
    val content: List<AnthropicContent>? = null,
    val error: AnthropicError? = null
)

@JsonClass(generateAdapter = true)
data class AnthropicContent(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class AnthropicError(
    val message: String? = null
)

// --- OpenAI Models ---
@JsonClass(generateAdapter = true)
data class OpenAiRequest(
    val model: String = "gpt-5.5-instant",
    val messages: List<OpenAiMessage>
)

@JsonClass(generateAdapter = true)
data class OpenAiMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class OpenAiResponse(
    val choices: List<OpenAiChoice>? = null,
    val error: OpenAiError? = null
)

@JsonClass(generateAdapter = true)
data class OpenAiChoice(
    val message: OpenAiMessage? = null
)

@JsonClass(generateAdapter = true)
data class OpenAiError(
    val message: String? = null
)

// --- Gemini Models ---
@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val tools: List<GeminiTool>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiTool(
    val googleSearch: GeminiGoogleSearch? = null,
    @Json(name = "google_search") val google_search: GeminiGoogleSearch? = null
)

@JsonClass(generateAdapter = true)
class GeminiGoogleSearch

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    @Json(name = "mime_type") val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null,
    @Json(name = "inline_data") val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,
    val error: GeminiError? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent? = null,
    val groundingMetadata: GeminiGroundingMetadata? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGroundingMetadata(
    val webSearchQueries: List<String>? = null,
    val groundingChunks: List<GeminiGroundingChunk>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGroundingChunk(
    val web: GeminiWebChunk? = null
)

@JsonClass(generateAdapter = true)
data class GeminiWebChunk(
    val uri: String? = null,
    val title: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiError(
    val message: String? = null
)
