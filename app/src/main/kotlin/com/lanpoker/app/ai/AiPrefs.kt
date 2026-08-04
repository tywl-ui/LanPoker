package com.lanpoker.app.ai

import android.content.Context
import android.content.SharedPreferences
import com.lanpoker.core.ai.AiConfig

/** AI 配置持久化（本机存储，API Key 不随工程上传） */
object AiPrefs {
    private const val FILE = "ai_config"
    private const val KEY_URL = "base_url"
    private const val KEY_KEY = "api_key"
    private const val KEY_MODEL = "model"
    private const val KEY_USE_LLM = "use_llm"

    fun load(context: Context): AiConfig {
        val sp = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return AiConfig(
            baseUrl = sp.getString(KEY_URL, "") ?: "",
            apiKey = sp.getString(KEY_KEY, "") ?: "",
            model = sp.getString(KEY_MODEL, "") ?: "",
            useLlm = sp.getBoolean(KEY_USE_LLM, true),
        )
    }

    fun save(context: Context, config: AiConfig) {
        val sp: SharedPreferences =
            context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        sp.edit()
            .putString(KEY_URL, config.baseUrl.trim())
            .putString(KEY_KEY, config.apiKey.trim())
            .putString(KEY_MODEL, config.model.trim())
            .putBoolean(KEY_USE_LLM, config.useLlm)
            .apply()
    }
}
