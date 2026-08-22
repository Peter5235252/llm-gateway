package com.example.ui

import android.app.Application
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SettingsRepository
import com.example.network.*
import com.example.util.FileHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

enum class LlmProvider {
    ANTHROPIC, OPENAI, GEMINI_PRO, GEMINI_FLASH, MISTRAL, CUSTOM
}

enum class LlmModel(val provider: LlmProvider, val modelId: String, val displayName: String, val isFree: Boolean) {
    CLAUDE_SONNET_5(LlmProvider.ANTHROPIC, "claude-sonnet-5", "Claude Sonnet 5", false),
    CLAUDE_FABLE_5(LlmProvider.ANTHROPIC, "claude-fable-5", "Claude Fable 5", false),
    CLAUDE_OPUS_5(LlmProvider.ANTHROPIC, "claude-opus-5", "Claude Opus 5", false),

    GPT_5_6_LUNA(LlmProvider.OPENAI, "gpt-5.6-luna", "GPT-5.6 Luna", false),
    GPT_5_6_TERRA(LlmProvider.OPENAI, "gpt-5.6-terra", "GPT-5.6 Terra", false),
    GPT_5_6_SOL(LlmProvider.OPENAI, "gpt-5.6-sol", "GPT-5.6 Sol", false),

    GEMINI_FLASH(LlmProvider.GEMINI_FLASH, "gemini-3.7-flash", "Gemini 3.7 Flash", true),
    GEMINI_PRO(LlmProvider.GEMINI_PRO, "gemini-3.1-pro-preview", "Gemini 3.1 Pro", true),
    
    MISTRAL_LARGE_3(LlmProvider.MISTRAL, "mistral-large-latest", "Mistral Large 3", false),
    MISTRAL_SMALL_3(LlmProvider.MISTRAL, "mistral-small-latest", "Mistral Small 3", false),
    
    CUSTOM_MODEL(LlmProvider.CUSTOM, "custom", "Custom Model", false)
}

data class Attachment(
    val name: String,
    val mimeType: String,
    val uriString: String,
    val base64Data: String? = null,
    val textContent: String? = null
)

data class GroundingSource(
    val title: String,
    val url: String
)

data class GeminiCallResult(
    val text: String,
    val sources: List<GroundingSource> = emptyList(),
    val searchQueries: List<String> = emptyList()
)

data class ChatMessage(
    val role: String, // "user", "model" / "assistant", "system"
    val content: String,
    val attachment: Attachment? = null,
    val groundingSources: List<GroundingSource> = emptyList(),
    val webSearchQueries: List<String> = emptyList(),
    val id: String = java.util.UUID.randomUUID().toString()
)

class GatewayViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val settingsRepo = SettingsRepository(application)
    private val fileHelper = FileHelper(application)
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _selectedProvider = MutableStateFlow(LlmProvider.GEMINI_FLASH)
    val selectedProvider: StateFlow<LlmProvider> = _selectedProvider.asStateFlow()

    private val _selectedModel = MutableStateFlow(LlmModel.GEMINI_FLASH)
    val selectedModel: StateFlow<LlmModel> = _selectedModel.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isBrowsingWeb = MutableStateFlow(false)
    val isBrowsingWeb: StateFlow<Boolean> = _isBrowsingWeb.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun clearError() { _error.value = null }

    // Force grounding for next Gemini response (globe icon)
    private val _forceGroundingNext = MutableStateFlow(false)
    val forceGroundingNext: StateFlow<Boolean> = _forceGroundingNext.asStateFlow()

    fun toggleForceGrounding() {
        // Only relevant for Gemini models - but allow toggle always, UI will hint
        _forceGroundingNext.value = !_forceGroundingNext.value
    }

    fun clearForceGrounding() {
        _forceGroundingNext.value = false
    }

    private val _selectedAttachment = MutableStateFlow<Attachment?>(null)
    val selectedAttachment: StateFlow<Attachment?> = _selectedAttachment.asStateFlow()

    private val _speakingMessageId = MutableStateFlow<String?>(null)
    val speakingMessageId: StateFlow<String?> = _speakingMessageId.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    val anthropicKey = MutableStateFlow(settingsRepo.getAnthropicKey())
    val openAiKey = MutableStateFlow(settingsRepo.getOpenAiKey())
    val geminiKey = MutableStateFlow(settingsRepo.getGeminiKey())
    val mistralKey = MutableStateFlow(settingsRepo.getMistralKey())
    val customKey = MutableStateFlow(settingsRepo.getCustomKey())
    val customBaseUrl = MutableStateFlow(settingsRepo.getCustomBaseUrl())
    val customModelId = MutableStateFlow(settingsRepo.getCustomModelId())
    val workingDirUri = MutableStateFlow(settingsRepo.getWorkingDirUri())
    val isDarkMode = MutableStateFlow(settingsRepo.isDarkMode())

    init {
        tts = TextToSpeech(application, this)
        
        // Auto-fill API keys from AI Studio Secrets in Preview environment (DEBUG build)
        if (com.example.BuildConfig.DEBUG) {
            val buildGemini = com.example.BuildConfig.GEMINI_API_KEY.trim()
            if (geminiKey.value.isEmpty() && buildGemini.isNotEmpty() && buildGemini != "MY_GEMINI_API_KEY") {
                updateGeminiKey(buildGemini)
            }
            
            val buildAnthropic = com.example.BuildConfig.ANTHROPIC_API_KEY.trim()
            if (anthropicKey.value.isEmpty() && buildAnthropic.isNotEmpty() && buildAnthropic != "MY_ANTHROPIC_API_KEY") {
                updateAnthropicKey(buildAnthropic)
            }
            
            val buildOpenAi = com.example.BuildConfig.OPENAI_API_KEY.trim()
            if (openAiKey.value.isEmpty() && buildOpenAi.isNotEmpty() && buildOpenAi != "MY_OPENAI_API_KEY") {
                updateOpenAiKey(buildOpenAi)
            }
            
            val buildMistral = com.example.BuildConfig.MISTRAL_API_KEY.trim()
            if (mistralKey.value.isEmpty() && buildMistral.isNotEmpty() && buildMistral != "MY_MISTRAL_API_KEY") {
                updateMistralKey(buildMistral)
            }
            
            val buildCustom = com.example.BuildConfig.CUSTOM_API_KEY.trim()
            val customTarget = if (buildCustom.isNotEmpty() && buildCustom != "MY_CUSTOM_API_KEY") buildCustom else ""
            if (customKey.value.isEmpty() && customTarget.isNotEmpty()) {
                updateCustomKey(customTarget)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            isTtsInitialized = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _speakingMessageId.value = utteranceId
                }

                override fun onDone(utteranceId: String?) {
                    if (_speakingMessageId.value == utteranceId) {
                        _speakingMessageId.value = null
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (_speakingMessageId.value == utteranceId) {
                        _speakingMessageId.value = null
                    }
                }
            })
        }
    }

    fun speakText(messageId: String, text: String) {
        if (!isTtsInitialized || tts == null) return

        if (_speakingMessageId.value == messageId) {
            tts?.stop()
            _speakingMessageId.value = null
        } else {
            tts?.stop()
            _speakingMessageId.value = messageId
            // Strip markdown symbols and code blocks for fluid speech
            val cleanText = text
                .replace(Regex("```[a-zA-Z0-9]*\\n[\\s\\S]*?```"), " code block omitted ")
                .replace(Regex("[#*_`~>\\[\\]]"), "")
                .replace(Regex("\\((http[^)]+)\\)"), "")
                .trim()

            val params = Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, messageId)
            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, messageId)
        }
    }

    fun stopSpeaking() {
        tts?.stop()
        _speakingMessageId.value = null
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
    
    fun toggleDarkMode() {
        val newVal = !isDarkMode.value
        isDarkMode.value = newVal
        settingsRepo.setDarkMode(newVal)
    }

    fun updateAnthropicKey(key: String) {
        val trimmed = key.trim()
        anthropicKey.value = trimmed
        settingsRepo.setAnthropicKey(trimmed)
    }

    fun updateOpenAiKey(key: String) {
        val trimmed = key.trim()
        openAiKey.value = trimmed
        settingsRepo.setOpenAiKey(trimmed)
    }

    fun updateGeminiKey(key: String) {
        val trimmed = key.trim()
        geminiKey.value = trimmed
        settingsRepo.setGeminiKey(trimmed)
    }
    
    fun updateMistralKey(key: String) {
        val trimmed = key.trim()
        mistralKey.value = trimmed
        settingsRepo.setMistralKey(trimmed)
    }
    
    fun updateCustomKey(key: String) {
        val trimmed = key.trim()
        customKey.value = trimmed
        settingsRepo.setCustomKey(trimmed)
    }

    fun updateCustomBaseUrl(url: String) {
        val trimmed = url.trim()
        customBaseUrl.value = trimmed
        settingsRepo.setCustomBaseUrl(trimmed)
    }

    fun updateCustomModelId(modelId: String) {
        val trimmed = modelId.trim()
        customModelId.value = trimmed
        settingsRepo.setCustomModelId(trimmed)
    }
    
    fun updateWorkingDirUri(uri: String) {
        workingDirUri.value = uri
        settingsRepo.setWorkingDirUri(uri)
    }

    fun selectModel(model: LlmModel) {
        _selectedModel.value = model
        _selectedProvider.value = model.provider
    }

    fun selectProvider(provider: LlmProvider) {
        _selectedProvider.value = provider
        _selectedModel.value = when (provider) {
            LlmProvider.ANTHROPIC -> LlmModel.CLAUDE_SONNET_5
            LlmProvider.OPENAI -> LlmModel.GPT_5_6_SOL
            LlmProvider.GEMINI_FLASH -> LlmModel.GEMINI_FLASH
            LlmProvider.GEMINI_PRO -> LlmModel.GEMINI_PRO
            LlmProvider.MISTRAL -> LlmModel.MISTRAL_LARGE_3
            LlmProvider.CUSTOM -> LlmModel.CUSTOM_MODEL
        }
    }

    private val _pendingIntentUri = MutableStateFlow<String?>(null)
    val pendingIntentUri: StateFlow<String?> = _pendingIntentUri.asStateFlow()

    fun clearPendingIntent() {
        _pendingIntentUri.value = null
    }

    fun processSelectedUri(uri: android.net.Uri, mimeType: String?, name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val resolvedMimeType = mimeType ?: getApplication<Application>().contentResolver.getType(uri) ?: "application/octet-stream"
                val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes != null) {
                    if (resolvedMimeType.startsWith("image/")) {
                        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        _selectedAttachment.value = Attachment(name, resolvedMimeType, uri.toString(), base64Data = base64)
                    } else {
                        val text = String(bytes)
                        _selectedAttachment.value = Attachment(name, resolvedMimeType, uri.toString(), textContent = text)
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to load attachment: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearAttachment() {
        _selectedAttachment.value = null
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank() && _selectedAttachment.value == null) return
        
        // Command check
        if (userText.startsWith("/export ")) {
            val fileName = userText.removePrefix("/export ").trim()
            exportChat(fileName)
            return
        }
        if (userText.startsWith("/read ")) {
            val fileName = userText.removePrefix("/read ").trim()
            readFile(fileName)
            return
        }

        val attachment = _selectedAttachment.value
        clearAttachment() // Consume attachment

        val newUserMsg = ChatMessage(role = "user", content = userText, attachment = attachment)
        _messages.value = _messages.value + newUserMsg
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val modelVal = _selectedModel.value
            val isGemini = modelVal.provider == LlmProvider.GEMINI_FLASH || modelVal.provider == LlmProvider.GEMINI_PRO
            // Grounding enabled for both Gemini models; globe forces it for next Gemini response (one-shot, consumed)
            val forceActive = _forceGroundingNext.value
            val enableSearch = isGemini // both 3.1 Pro and 3.7 Flash now support grounding; force flag consumed for UX feedback
            if (enableSearch) _isBrowsingWeb.value = true
            if (forceActive) _forceGroundingNext.value = false
            
            try {
                val (responseContent, sources, queries) = when (modelVal.provider) {
                    LlmProvider.ANTHROPIC -> Triple(callAnthropic(modelVal.modelId, _messages.value), emptyList<GroundingSource>(), emptyList<String>())
                    LlmProvider.OPENAI -> Triple(callOpenAi(modelVal.modelId, _messages.value), emptyList<GroundingSource>(), emptyList<String>())
                    LlmProvider.GEMINI_PRO -> {
                        val res = callGemini(modelVal.modelId, _messages.value, enableSearch = enableSearch)
                        Triple(res.text, res.sources, res.searchQueries)
                    }
                    LlmProvider.GEMINI_FLASH -> {
                        val res = callGemini(modelVal.modelId, _messages.value, enableSearch = enableSearch)
                        Triple(res.text, res.sources, res.searchQueries)
                    }
                    LlmProvider.MISTRAL -> {
                        val key = mistralKey.value
                        val id = modelVal.modelId
                        Triple(callOpenAi(id, _messages.value, overrideUrl = "https://api.mistral.ai/v1/chat/completions", overrideKey = key), emptyList<GroundingSource>(), emptyList<String>())
                    }
                    LlmProvider.CUSTOM -> {
                        val url = customBaseUrl.value
                        val key = customKey.value
                        val id = customModelId.value
                        Triple(callOpenAi(id, _messages.value, overrideUrl = url, overrideKey = key), emptyList<GroundingSource>(), emptyList<String>())
                    }
                }
                
                val newModelMsg = ChatMessage(
                    role = "model",
                    content = responseContent,
                    groundingSources = sources,
                    webSearchQueries = queries
                )
                _messages.value = _messages.value + newModelMsg
                
                // Parse for intent commands
                parseForIntentCommand(responseContent)
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
                _isBrowsingWeb.value = false
            }
        }
    }

    fun regenerateResponse(targetMessageId: String? = null) {
        val currentList = _messages.value
        if (currentList.isEmpty() || _isLoading.value) return

        // Find the index of the model message to regenerate or default to last model message
        val targetIndex = if (targetMessageId != null) {
            currentList.indexOfFirst { it.id == targetMessageId }
        } else {
            currentList.indexOfLast { it.role == "model" || it.role == "assistant" }
        }

        if (targetIndex < 0) return

        // Truncate messages up to the model response being regenerated
        val historyForGeneration = currentList.subList(0, targetIndex)
        _messages.value = historyForGeneration

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val modelVal = _selectedModel.value
            val isGeminiRegen = modelVal.provider == LlmProvider.GEMINI_FLASH || modelVal.provider == LlmProvider.GEMINI_PRO
            val forceActiveRegen = _forceGroundingNext.value
            val enableSearchRegen = isGeminiRegen // both 3.1 Pro and 3.7 Flash now support grounding
            if (enableSearchRegen) _isBrowsingWeb.value = true
            if (forceActiveRegen) _forceGroundingNext.value = false

            try {
                val (responseContent, sources, queries) = when (modelVal.provider) {
                    LlmProvider.ANTHROPIC -> Triple(callAnthropic(modelVal.modelId, historyForGeneration), emptyList<GroundingSource>(), emptyList<String>())
                    LlmProvider.OPENAI -> Triple(callOpenAi(modelVal.modelId, historyForGeneration), emptyList<GroundingSource>(), emptyList<String>())
                    LlmProvider.GEMINI_PRO -> {
                        val res = callGemini(modelVal.modelId, historyForGeneration, enableSearch = enableSearchRegen)
                        Triple(res.text, res.sources, res.searchQueries)
                    }
                    LlmProvider.GEMINI_FLASH -> {
                        val res = callGemini(modelVal.modelId, historyForGeneration, enableSearch = enableSearchRegen)
                        Triple(res.text, res.sources, res.searchQueries)
                    }
                    LlmProvider.MISTRAL -> {
                        val key = mistralKey.value
                        val id = modelVal.modelId
                        Triple(callOpenAi(id, historyForGeneration, overrideUrl = "https://api.mistral.ai/v1/chat/completions", overrideKey = key), emptyList<GroundingSource>(), emptyList<String>())
                    }
                    LlmProvider.CUSTOM -> {
                        val url = customBaseUrl.value
                        val key = customKey.value
                        val id = customModelId.value
                        Triple(callOpenAi(id, historyForGeneration, overrideUrl = url, overrideKey = key), emptyList<GroundingSource>(), emptyList<String>())
                    }
                }

                val newModelMsg = ChatMessage(
                    role = "model",
                    content = responseContent,
                    groundingSources = sources,
                    webSearchQueries = queries
                )
                _messages.value = historyForGeneration + newModelMsg

                parseForIntentCommand(responseContent)
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
                _isBrowsingWeb.value = false
            }
        }
    }
    
    private fun parseForIntentCommand(content: String) {
        try {
            if (content.contains("\"action\": \"ACTION_VIEW\"") && content.contains("\"url\":")) {
                val urlRegex = "\"url\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                val match = urlRegex.find(content)
                if (match != null) {
                    val url = match.groupValues[1]
                    _pendingIntentUri.value = url
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun exportChat(fileName: String) {
        val uriStr = workingDirUri.value
        if (uriStr.isEmpty()) {
            _error.value = "Working directory not set in settings."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val content = _messages.value.joinToString("\n\n") { "${it.role}: ${it.content}" }
            val success = fileHelper.saveFile(uriStr, fileName, content)
            _isLoading.value = false
            if (success) {
                _messages.value = _messages.value + ChatMessage("system", "Successfully exported chat to $fileName")
            } else {
                _error.value = "Failed to export chat."
            }
        }
    }
    
    private fun readFile(fileName: String) {
        val uriStr = workingDirUri.value
        if (uriStr.isEmpty()) {
            _error.value = "Working directory not set in settings."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val content = fileHelper.readFile(uriStr, fileName)
            _isLoading.value = false
            if (content != null) {
                _messages.value = _messages.value + ChatMessage("system", "Read file $fileName:\n$content")
            } else {
                _error.value = "Failed to read file $fileName."
            }
        }
    }

    // Edit / Revert user messages - shown when user highlights their own message
    fun editUserMessage(messageId: String, newContent: String) {
        val trimmed = newContent.trim()
        if (trimmed.isEmpty()) return
        val current = _messages.value.toMutableList()
        val idx = current.indexOfFirst { it.id == messageId && it.role == "user" }
        if (idx == -1) return
        val old = current[idx]
        current[idx] = old.copy(content = trimmed)
        _messages.value = current
    }

    fun revertUserMessage(messageId: String) {
        val current = _messages.value.toMutableList()
        val idx = current.indexOfFirst { it.id == messageId }
        if (idx == -1) return
        // Remove the user message
        current.removeAt(idx)
        // If next message is a model/assistant response, remove it as well (revert the exchange)
        if (idx < current.size) {
            val next = current[idx]
            if (next.role == "model" || next.role == "assistant") {
                current.removeAt(idx)
            }
        }
        _messages.value = current
        // Stop any ongoing TTS that might be related
        stopSpeaking()
    }

    private suspend fun callAnthropic(modelId: String, history: List<ChatMessage>): String {
        val buildConfigKey = com.example.BuildConfig.ANTHROPIC_API_KEY.trim()
        val defaultKey = if (buildConfigKey != "MY_ANTHROPIC_API_KEY") buildConfigKey else ""
        val key = anthropicKey.value.trim().ifEmpty { defaultKey }
        if (key.isEmpty()) throw Exception("Anthropic key is missing. Enter it in Settings or via AI Studio Secrets panel.")

        
        val mappedMessages = history.filter { it.role != "system" }.map { msg ->
            val promptContent = if (msg.attachment?.textContent != null) {
                "${msg.content}\n\n[File Attachment: ${msg.attachment.name}]\n${msg.attachment.textContent}"
            } else {
                msg.content
            }
            AnthropicMessage(
                role = if (msg.role == "model") "assistant" else msg.role,
                content = promptContent
            )
        }
        val request = AnthropicRequest(model = modelId, messages = mappedMessages)
        val response = LlmClient.apiService.sendAnthropicMessage(apiKey = key, request = request)
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code()}: ${response.errorBody()?.string()}")
        }
        return response.body()?.content?.firstOrNull()?.text ?: "No response"
    }

    private suspend fun callOpenAi(modelId: String, history: List<ChatMessage>, overrideUrl: String? = null, overrideKey: String? = null): String {
        val buildConfigKey = if (overrideUrl != null) {
            if (overrideUrl.contains("mistral")) {
                com.example.BuildConfig.MISTRAL_API_KEY.trim()
            } else {
                com.example.BuildConfig.CUSTOM_API_KEY.trim()
            }
        } else {
            com.example.BuildConfig.OPENAI_API_KEY.trim()
        }
        val defaultKey = if (buildConfigKey != "MY_CUSTOM_API_KEY" && buildConfigKey != "MY_OPENAI_API_KEY" && buildConfigKey != "MY_MISTRAL_API_KEY") buildConfigKey else ""
        val providedKey = (overrideKey ?: openAiKey.value).trim()
        val key = providedKey.ifEmpty { defaultKey }
        if (key.isEmpty()) throw Exception("API key is missing. Enter it in Settings or via AI Studio Secrets panel.")

        
        val mappedMessages = history.filter { it.role != "system" }.map { msg ->
            val promptContent = if (msg.attachment?.textContent != null) {
                "${msg.content}\n\n[File Attachment: ${msg.attachment.name}]\n${msg.attachment.textContent}"
            } else {
                msg.content
            }
            OpenAiMessage(
                role = if (msg.role == "model") "assistant" else msg.role,
                content = promptContent
            )
        }
        val request = OpenAiRequest(model = modelId, messages = mappedMessages)
        val finalUrl = if (overrideUrl != null) {
            val trimmedUrl = overrideUrl.trim()
            if (!trimmedUrl.endsWith("/chat/completions") && !trimmedUrl.endsWith("/completions")) {
                if (trimmedUrl.endsWith("/")) "${trimmedUrl}chat/completions" else "$trimmedUrl/chat/completions"
            } else {
                trimmedUrl
            }
        } else {
            null
        }
        
        val response = if (finalUrl != null) {
            LlmClient.apiService.sendOpenAiMessage(url = finalUrl, authorization = "Bearer $key", request = request)
        } else {
            LlmClient.apiService.sendOpenAiMessage(authorization = "Bearer $key", request = request)
        }
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code()}: ${response.errorBody()?.string()}")
        }
        return response.body()?.choices?.firstOrNull()?.message?.content ?: "No response"
    }

    private suspend fun callGemini(model: String, history: List<ChatMessage>, enableSearch: Boolean = false): GeminiCallResult {
        val buildConfigKey = com.example.BuildConfig.GEMINI_API_KEY.trim()
        val defaultKey = if (buildConfigKey != "MY_GEMINI_API_KEY") buildConfigKey else ""
        val key = geminiKey.value.trim().ifEmpty { defaultKey }
        if (key.isEmpty()) throw Exception("Gemini key is missing. Please enter your API Key in Settings or set it up in AI Studio.")

        
        val mappedContents = history.filter { it.role != "system" }.map { msg ->
            val partsList = mutableListOf<GeminiPart>()
            
            val promptContent = if (msg.attachment?.textContent != null) {
                "${msg.content}\n\n[File Attachment: ${msg.attachment.name}]\n${msg.attachment.textContent}"
            } else {
                msg.content
            }
            if (promptContent.isNotEmpty()) {
                partsList.add(GeminiPart(text = promptContent))
            }
            
            if (msg.attachment?.base64Data != null) {
                partsList.add(
                    GeminiPart(
                        inlineData = GeminiInlineData(
                            mimeType = msg.attachment.mimeType,
                            data = msg.attachment.base64Data
                        )
                    )
                )
            }
            
            GeminiContent(
                role = msg.role,
                parts = partsList
            )
        }
        val tools = if (enableSearch) listOf(GeminiTool(googleSearch = GeminiGoogleSearch())) else null
        val request = GeminiRequest(contents = mappedContents, tools = tools)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key"
        val response = LlmClient.apiService.sendGeminiMessage(url = url, request = request)
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code()}: ${response.errorBody()?.string()}")
        }
        val candidate = response.body()?.candidates?.firstOrNull()
        val text = candidate?.content?.parts?.firstOrNull()?.text ?: "No response"
        val groundingMeta = candidate?.groundingMetadata
        val queries = groundingMeta?.webSearchQueries ?: emptyList()
        val sources = groundingMeta?.groundingChunks?.mapNotNull { chunk ->
            val uri = chunk.web?.uri
            val title = chunk.web?.title ?: uri
            if (uri != null) GroundingSource(title = title ?: "Web Source", url = uri) else null
        } ?: emptyList()

        return GeminiCallResult(text = text, sources = sources, searchQueries = queries)
    }
}
