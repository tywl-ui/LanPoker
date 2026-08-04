package com.lanpoker.app.ui.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.lanpoker.app.ai.AiPrefs
import com.lanpoker.core.ai.AiConfig
import com.lanpoker.core.ai.LlmClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AiSettingsState(
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val useLlm: Boolean = true,
)

class AiSettingsViewModel : ViewModel() {

    var state by mutableStateOf(AiSettingsState())
        private set

    init {
        state = AiSettingsState() // 由 screen 加载？不：这里留空，screen 初次用 prefs 填充
    }

    fun load(context: Context) {
        val c = AiPrefs.load(context)
        state = AiSettingsState(c.baseUrl, c.apiKey, c.model, c.useLlm)
    }

    fun setBaseUrl(v: String) { state = state.copy(baseUrl = v) }
    fun setApiKey(v: String) { state = state.copy(apiKey = v) }
    fun setModel(v: String) { state = state.copy(model = v) }
    fun setUseLlm(v: Boolean) { state = state.copy(useLlm = v) }

    fun save(context: Context) {
        AiPrefs.save(context, AiConfig(state.baseUrl, state.apiKey, state.model, state.useLlm))
    }

    suspend fun testConnection(context: Context): Boolean {
        val cfg = AiConfig(state.baseUrl, state.apiKey, state.model, useLlm = true)
        if (!cfg.isUsable()) return false
        return withContext(Dispatchers.IO) {
            try {
                val client = LlmClient(cfg)
                val reply = client.chat("你是一个测试助手", "回复：OK")
                !reply.isNullOrBlank()
            } catch (e: Exception) {
                false
            }
        }
    }
}
