package com.lanpoker.core.ai

/** AI 决策动作 */
enum class AiActionType { LOOK, CALL, RAISE, FOLD, COMPARE }

data class AiDecision(
    val action: AiActionType,
    val level: Int? = null,
    val targetId: Int? = null,
    val reason: String = "",
)

/** 大模型 API 配置（OpenAI 兼容接口） */
data class AiConfig(
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val useLlm: Boolean = true,
) {
    fun isUsable(): Boolean = useLlm && baseUrl.isNotBlank() && apiKey.isNotBlank() && model.isNotBlank()
}
