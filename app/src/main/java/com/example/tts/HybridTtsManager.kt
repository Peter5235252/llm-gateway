package com.example.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.example.data.SettingsRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class HybridTtsManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope
) : TextToSpeech.OnInitListener {

    private var systemTts: TextToSpeech? = null
    private var isSystemReady = false
    private var mediaPlayer: MediaPlayer? = null
    private var onStart: ((String?) -> Unit)? = null
    private var onDone: ((String?) -> Unit)? = null
    private var onError: ((String?) -> Unit)? = null
    private var currentUtteranceId: String? = null

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
    private val cloudService: CloudTtsService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CloudTtsService::class.java)
    }

    fun init(
        onStartCb: (String?) -> Unit = {},
        onDoneCb: (String?) -> Unit = {},
        onErrorCb: (String?) -> Unit = {}
    ) {
        onStart = onStartCb
        onDone = onDoneCb
        onError = onErrorCb
        try {
            systemTts = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e("HybridTts", "TTS init failed", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val locale = Locale.getDefault()
            // Prefer English as default but respect system locale for multilingual
            val targetLocale = if (locale.language in listOf("en", "es", "fr", "de", "it", "pt", "ja", "ko", "zh", "ru")) locale else Locale.US
            systemTts?.language = targetLocale
            isSystemReady = true
            try {
                val voices = systemTts?.voices
                // Prefer offline natural voices: look for piper/kokoro/neural/enhanced/wave with high quality
                val bestVoice = selectNaturalVoice(voices, targetLocale)
                if (bestVoice != null) {
                    systemTts?.voice = bestVoice
                    Log.i("HybridTts", "Selected natural offline voice: ${bestVoice.name} q=${bestVoice.quality} locale=${bestVoice.locale}")
                } else {
                    Log.i("HybridTts", "No enhanced voice found, using default")
                }
                systemTts?.setSpeechRate(0.96f)
                systemTts?.setPitch(1.02f)
            } catch (e: Exception) {
                Log.w("HybridTts", "Voice selection failed", e)
            }
            systemTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    currentUtteranceId = utteranceId
                    onStart?.invoke(utteranceId)
                }
                override fun onDone(utteranceId: String?) {
                    if (currentUtteranceId == utteranceId) currentUtteranceId = null
                    onDone?.invoke(utteranceId)
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (currentUtteranceId == utteranceId) currentUtteranceId = null
                    onError?.invoke(utteranceId)
                }
                override fun onError(utteranceId: String?, errorCode: Int) {
                    if (currentUtteranceId == utteranceId) currentUtteranceId = null
                    onError?.invoke(utteranceId)
                }
            })
        } else {
            Log.e("HybridTts", "System TTS init failed status=$status")
        }
    }

    private fun selectNaturalVoice(voices: Set<Voice>?, targetLocale: Locale): Voice? {
        if (voices.isNullOrEmpty()) return null
        val offlineVoices = voices.filter { !it.isNetworkConnectionRequired }
        // Score voices: quality + name contains natural keywords + locale match
        fun score(v: Voice): Int {
            var s = v.quality * 10
            val name = v.name.lowercase()
            if (name.contains("enhanced")) s += 500
            if (name.contains("neural")) s += 400
            if (name.contains("wave")) s += 300
            if (name.contains("kokoro")) s += 350
            if (name.contains("piper")) s += 200
            if (name.contains("premium")) s += 150
            if (v.locale.language == targetLocale.language) s += 100
            if (v.locale == targetLocale) s += 50
            // Prefer offline for privacy, but high quality cloud voices are ok if offline not needed
            if (!v.isNetworkConnectionRequired) s += 20
            return s
        }
        return (offlineVoices.ifEmpty { voices.toList() })
            .sortedByDescending { score(it) }
            .firstOrNull()
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                 caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                 caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        } catch (e: Exception) {
            false
        }
    }

    private fun getCloudKey(): String {
        // Prefer OpenAI key, fallback to custom
        val openAi = settingsRepository.getOpenAiKey().trim()
        if (openAi.isNotEmpty()) return openAi
        val custom = settingsRepository.getCustomKey().trim()
        if (custom.isNotEmpty()) return custom
        // Also check BuildConfig fallback
        return try {
            val field = com.example.BuildConfig::class.java.getField("OPENAI_API_KEY")
            val v = field.get(null) as String
            if (v != "MY_OPENAI_API_KEY") v.trim() else ""
        } catch (e: Exception) { "" }
    }

    fun speak(
        text: String,
        utteranceId: String,
        onStartCb: ((String?) -> Unit)? = null,
        onDoneCb: ((String?) -> Unit)? = null,
        onErrorCb: ((String?) -> Unit)? = null
    ) {
        // Clean text for more natural prosody (similar to previous but keep for cloud too)
        var cleanText = text
            .replace(Regex("```[a-zA-Z0-9]*\\n[\\s\\S]*?```"), " . Code block omitted. ")
            .replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("\\*(.*?)\\*"), "$1")
            .replace(Regex("`([^`]+)`"), "$1")
            .replace(Regex("\\[([^\\]]+)]\\((http[^)]+)\\)"), "$1")
            .replace(Regex("[*_`~>\\[\\]]"), "")
            .replace(Regex("\\n{2,}"), ". ")
            .replace(Regex("\\n"), " ")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
        cleanText = cleanText.replace(Regex("([.!?])\\s+"), "$1  ")
        if (cleanText.isBlank()) {
            onDoneCb?.invoke(utteranceId) ?: onDone?.invoke(utteranceId)
            return
        }

        val mode = settingsRepository.getTtsMode() // auto, offline, cloud
        val canUseCloud = when (mode) {
            "offline" -> false
            "cloud" -> true
            else -> true // auto
        } && isNetworkAvailable() && getCloudKey().isNotEmpty()

        // If cloud is preferred and available, try cloud first
        if (canUseCloud) {
            scope.launch {
                val success = tryCloudTts(cleanText, utteranceId, onStartCb, onDoneCb, onErrorCb)
                if (!success) {
                    // Fallback to offline on cloud failure
                    withContext(Dispatchers.Main) {
                        speakOffline(cleanText, utteranceId, onStartCb, onDoneCb, onErrorCb)
                    }
                }
            }
        } else {
            speakOffline(cleanText, utteranceId, onStartCb, onDoneCb, onErrorCb)
        }
    }

    private suspend fun tryCloudTts(
        text: String,
        utteranceId: String,
        onStartCb: ((String?) -> Unit)?,
        onDoneCb: ((String?) -> Unit)?,
        onErrorCb: ((String?) -> Unit)?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val key = getCloudKey()
            if (key.isEmpty()) return@withContext false
            val baseUrl = settingsRepository.getCloudTtsBaseUrl().trim().ifEmpty { "https://api.openai.com/v1/audio/speech" }
            val model = settingsRepository.getCloudTtsModel().trim().ifEmpty { "tts-1" }
            val voice = settingsRepository.getCloudTtsVoice().trim().ifEmpty { "alloy" }
            // Detect language: default English, but OpenAI tts-1 handles multilingual via voice alloy (multilingual-ish)
            // For more natural multilingual, we could pick voice based on locale, but keep alloy as default English
            val request = CloudTtsRequest(
                model = model,
                input = text,
                voice = voice,
                response_format = "mp3",
                speed = 1.0
            )
            // Update UI to speaking
            withContext(Dispatchers.Main) {
                onStartCb?.invoke(utteranceId) ?: onStart?.invoke(utteranceId)
            }
            val response = cloudService.synthesize(
                url = baseUrl,
                authorization = "Bearer $key",
                request = request
            )
            if (!response.isSuccessful) {
                val err = response.errorBody()?.string() ?: "Unknown"
                Log.w("HybridTts", "Cloud TTS failed ${response.code()} $err, fallback to offline")
                withContext(Dispatchers.Main) {
                    onErrorCb?.invoke(utteranceId) ?: onError?.invoke(utteranceId)
                }
                return@withContext false
            }
            val body = response.body() ?: return@withContext false
            val bytes = body.bytes()
            if (bytes.isEmpty()) return@withContext false
            // Save to temp file and play via MediaPlayer
            val tempFile = File(context.cacheDir, "tts_${utteranceId}_${UUID.randomUUID()}.mp3")
            tempFile.writeBytes(bytes)
            withContext(Dispatchers.Main) {
                playMediaFile(tempFile, utteranceId, onStartCb, onDoneCb, onErrorCb)
            }
            return@withContext true
        } catch (e: Exception) {
            Log.w("HybridTts", "Cloud TTS exception, fallback", e)
            withContext(Dispatchers.Main) {
                onErrorCb?.invoke(utteranceId) ?: onError?.invoke(utteranceId)
            }
            return@withContext false
        }
    }

    private fun playMediaFile(
        file: File,
        utteranceId: String,
        onStartCb: ((String?) -> Unit)?,
        onDoneCb: ((String?) -> Unit)?,
        onErrorCb: ((String?) -> Unit)?
    ) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(file.absolutePath)
                setOnPreparedListener {
                    currentUtteranceId = utteranceId
                    onStartCb?.invoke(utteranceId) ?: onStart?.invoke(utteranceId)
                    start()
                }
                setOnCompletionListener {
                    currentUtteranceId = null
                    onDoneCb?.invoke(utteranceId) ?: onDone?.invoke(utteranceId)
                    try { file.delete() } catch (_: Exception) {}
                    release()
                    mediaPlayer = null
                }
                setOnErrorListener { _, _, _ ->
                    currentUtteranceId = null
                    onErrorCb?.invoke(utteranceId) ?: onError?.invoke(utteranceId)
                    try { file.delete() } catch (_: Exception) {}
                    release()
                    mediaPlayer = null
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("HybridTts", "MediaPlayer play failed", e)
            onErrorCb?.invoke(utteranceId) ?: onError?.invoke(utteranceId)
        }
    }

    private fun speakOffline(
        text: String,
        utteranceId: String,
        onStartCb: ((String?) -> Unit)?,
        onDoneCb: ((String?) -> Unit)?,
        onErrorCb: ((String?) -> Unit)?
    ) {
        if (!isSystemReady || systemTts == null) {
            Log.w("HybridTts", "System TTS not ready, trying to init")
            // Try to use cloud as last resort if offline not ready? Already tried cloud.
            onErrorCb?.invoke(utteranceId) ?: onError?.invoke(utteranceId)
            return
        }
        // Ensure natural voice settings are applied
        try {
            systemTts?.setSpeechRate(0.96f)
            systemTts?.setPitch(1.02f)
        } catch (_: Exception) {}
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        // Temporarily override listeners if custom callbacks provided
        if (onStartCb != null || onDoneCb != null || onErrorCb != null) {
            val prevStart = onStart
            val prevDone = onDone
            val prevError = onError
            // Set one-time listeners
            systemTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {
                    onStartCb?.invoke(id) ?: prevStart?.invoke(id)
                    // Restore after?
                }
                override fun onDone(id: String?) {
                    onDoneCb?.invoke(id) ?: prevDone?.invoke(id)
                    // Restore original listener
                    systemTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(i: String?) { prevStart?.invoke(i) }
                        override fun onDone(i: String?) { prevDone?.invoke(i) }
                        @Deprecated("Deprecated in Java")
                        override fun onError(i: String?) { prevError?.invoke(i) }
                        override fun onError(i: String?, errorCode: Int) { prevError?.invoke(i) }
                    })
                }
                @Deprecated("Deprecated in Java")
                override fun onError(id: String?) {
                    onErrorCb?.invoke(id) ?: prevError?.invoke(id)
                }
                override fun onError(id: String?, errorCode: Int) {
                    onErrorCb?.invoke(id) ?: prevError?.invoke(id)
                }
            })
        }
        currentUtteranceId = utteranceId
        val result = systemTts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (result == TextToSpeech.ERROR) {
            Log.e("HybridTts", "System TTS speak ERROR")
            onErrorCb?.invoke(utteranceId) ?: onError?.invoke(utteranceId)
        }
    }

    fun stop() {
        try {
            systemTts?.stop()
        } catch (_: Exception) {}
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {}
        currentUtteranceId = null
    }

    fun shutdown() {
        stop()
        try {
            systemTts?.shutdown()
        } catch (_: Exception) {}
        systemTts = null
        isSystemReady = false
    }

    fun isSpeaking(): Boolean {
        return try {
            systemTts?.isSpeaking == true || mediaPlayer?.isPlaying == true
        } catch (_: Exception) { false }
    }
}
