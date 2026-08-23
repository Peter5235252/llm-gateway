package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SettingsRepository(context: Context) {

    private val sharedPreferences: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "secure_settings",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.w("SettingsRepository", "EncryptedSharedPreferences failed, falling back to standard SharedPreferences: ${e.message}")
        context.getSharedPreferences("gateway_settings_fallback", Context.MODE_PRIVATE)
    }

    fun getAnthropicKey(): String = sharedPreferences.getString("anthropic_key", "") ?: ""
    fun setAnthropicKey(key: String) = sharedPreferences.edit().putString("anthropic_key", key).apply()

    fun getOpenAiKey(): String = sharedPreferences.getString("openai_key", "") ?: ""
    fun setOpenAiKey(key: String) = sharedPreferences.edit().putString("openai_key", key).apply()

    fun getGeminiKey(): String = sharedPreferences.getString("gemini_key", "") ?: ""
    fun setGeminiKey(key: String) = sharedPreferences.edit().putString("gemini_key", key).apply()
    
    fun getMistralKey(): String = sharedPreferences.getString("mistral_key", "") ?: ""
    fun setMistralKey(key: String) = sharedPreferences.edit().putString("mistral_key", key).apply()

    fun getCustomKey(): String = sharedPreferences.getString("custom_key", "") ?: ""
    fun setCustomKey(key: String) = sharedPreferences.edit().putString("custom_key", key).apply()

    fun getCustomBaseUrl(): String = sharedPreferences.getString("custom_base_url", "https://api.openai.com/v1/chat/completions") ?: "https://api.openai.com/v1/chat/completions"
    fun setCustomBaseUrl(url: String) = sharedPreferences.edit().putString("custom_base_url", url).apply()

    fun getCustomModelId(): String = sharedPreferences.getString("custom_model_id", "") ?: ""
    fun setCustomModelId(modelId: String) = sharedPreferences.edit().putString("custom_model_id", modelId).apply()
    
    fun getWorkingDirUri(): String = sharedPreferences.getString("working_dir_uri", "") ?: ""
    fun setWorkingDirUri(uri: String) = sharedPreferences.edit().putString("working_dir_uri", uri).apply()

    fun isDarkMode(): Boolean = sharedPreferences.getBoolean("is_dark_mode", true)
    fun setDarkMode(enabled: Boolean) = sharedPreferences.edit().putBoolean("is_dark_mode", enabled).apply()
}

