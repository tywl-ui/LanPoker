package com.lanpoker.core.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * OpenAI 兼容的大模型客户端（DeepSeek / Qwen / GLM / 任意兼容服务均可）。
 * 纯 JDK 实现，不引入额外依赖。
 */
class LlmClient(
    private val config: AiConfig,
) {
    suspend fun chat(system: String, prompt: String): String? = withContext(Dispatchers.IO) {
        // 容错：用户可能粘贴了带 /v1 或完整 /chat/completions 的地址
        var base = config.baseUrl.trim().trimEnd('/')
        base = base.removeSuffix("/chat/completions")
        base = base.removeSuffix("/v1")
        val url = URL("$base/chat/completions")
        val body = buildString {
            append("{\"model\":\"").append(escape(config.model))
                .append("\",\"messages\":[{\"role\":\"system\",\"content\":\"").append(escape(system))
                .append("\"},{\"role\":\"user\",\"content\":\"").append(escape(prompt))
                .append("\"}],\"temperature\":1.2,\"max_tokens\":200}")
        }
        try {
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 8_000
            conn.readTimeout = 20_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            conn.disconnect()
            extractContent(text)
        } catch (e: Exception) {
            null
        }
    }

    private fun extractContent(json: String): String? {
        val regex = Regex("\"content\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
        val m = regex.find(json) ?: return null
        return m.groupValues[1]
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\u003c", "<")
            .replace("\\u003e", ">")
    }

    /** 兼容测试用的实例方法 */
    fun parseDecision(text: String?): AiDecision? = parseDecisionStatic(text)

    fun parseQuickLook(text: String?): Boolean? = parseQuickLookStatic(text)

    companion object {
        /** 从模型回答里解析出决策 JSON（容忍前后废话/代码块标记） */
        fun parseDecisionStatic(text: String?): AiDecision? {
            if (text.isNullOrBlank()) return null
            var json = text
            // 去掉 ```json ``` 包裹
            json = json.replace(Regex("```(?:json)?\\s*"), "").replace("```", "")
            val start = json.indexOf('{')
            val end = json.lastIndexOf('}')
            if (start < 0 || end <= start) return null
            json = json.substring(start, end + 1)

            fun field(name: String): String? =
                Regex("\"$name\"\\s*:\\s*\"?([^\",}\\s]+)").find(json)?.groupValues?.get(1)

            return when (field("action")) {
                "look" -> AiDecision(AiActionType.LOOK, reason = field("reason") ?: "")
                "call" -> AiDecision(AiActionType.CALL, reason = field("reason") ?: "")
                "fold" -> AiDecision(AiActionType.FOLD, reason = field("reason") ?: "")
                "raise" -> AiDecision(
                    AiActionType.RAISE,
                    level = field("level")?.toIntOrNull(),
                    reason = field("reason") ?: "",
                )
                "compare" -> AiDecision(
                    AiActionType.COMPARE,
                    targetId = field("target")?.toIntOrNull(),
                    reason = field("reason") ?: "",
                )
                else -> null
            }
        }

        fun parseQuickLookStatic(text: String?): Boolean? {
            if (text.isNullOrBlank()) return null
            var json = text
            json = json.replace(Regex("```(?:json)?\\s*"), "").replace("```", "")
            return Regex("\"look\"\\s*:\\s*(true|false)").find(json)?.groupValues?.get(1)?.toBoolean()
        }
    }

    private fun escape(s: String) = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "")
}
