package com.lanpoker.app.ai

import com.lanpoker.core.ai.AiConfig
import com.lanpoker.core.ai.AiDecision
import com.lanpoker.core.ai.HeuristicAi
import com.lanpoker.core.ai.LlmClient
import com.lanpoker.core.deck.Card
import com.lanpoker.core.zjh.ZjhBettingGame
import com.lanpoker.core.zjh.ZjhHand

/**
 * AI 决策引擎：配置了大模型 API 就走 LLM，否则用内置启发式 AI 兜底。
 */
class AiEngine(
    private val config: AiConfig,
) {
    private val llm: LlmClient? = if (config.isUsable()) LlmClient(config) else null

    /** 标准局决策 */
    suspend fun decideBetting(
        hand: ZjhHand,
        gs: ZjhBettingGame.State,
        myId: Int,
        players: List<com.lanpoker.core.ledger.Player>,
        base: Int,
        maxLevel: Int,
    ): AiDecision {
        val heuristic = HeuristicAi.decideBetting(hand, gs, myId, base, maxLevel)
        val llm = llm ?: return heuristic
        val answer = llm.chat(
            system = SYSTEM_PROMPT,
            prompt = buildBettingPrompt(hand, gs, myId, players, base),
        )
        val parsed = llm.parseDecision(answer)
        if (parsed == null || !validate(parsed, gs, myId, maxLevel, players)) return heuristic
        return parsed
    }

    /** 快局闷/看决策 */
    suspend fun decideQuickLook(hand: ZjhHand, gs: ZjhQuickSnapshot): Boolean {
        val strength = HeuristicAi.strength(hand)
        val heuristic = HeuristicAi.decideQuickLook(strength)
        val llm = llm ?: return heuristic
        val answer = llm.chat(
            system = SYSTEM_PROMPT,
            prompt = buildQuickPrompt(hand, gs),
        )
        val parsed = llm.parseQuickLook(answer)
        return parsed ?: heuristic
    }

    private fun validate(d: AiDecision, gs: ZjhBettingGame.State, myId: Int, maxLevel: Int, players: List<com.lanpoker.core.ledger.Player>): Boolean {
        val id = myId
        if (id in gs.folded) return false
        val level = d.level
        val target = d.targetId
        val activeCount = players.count { it.id !in gs.folded }
        return when (d.action) {
            com.lanpoker.core.ai.AiActionType.LOOK -> id !in gs.looked
            com.lanpoker.core.ai.AiActionType.CALL -> true
            com.lanpoker.core.ai.AiActionType.RAISE -> level != null && level > gs.level && level <= maxLevel
            com.lanpoker.core.ai.AiActionType.FOLD -> true
            com.lanpoker.core.ai.AiActionType.COMPARE ->
                target != null && target != id && target !in gs.folded &&
                    (activeCount <= 2 || target in gs.looked)
        }
    }

    private fun buildBettingPrompt(
        hand: ZjhHand,
        gs: ZjhBettingGame.State,
        myId: Int,
        players: List<com.lanpoker.core.ledger.Player>,
        base: Int,
    ): String {
        val sb = StringBuilder()
        sb.appendLine("这是炸金花对局，你是玩家「${players.first { it.id == myId }.name}」。")
        sb.appendLine("底分 = $base 分。当前 level = ${gs.level}（闷牌跟注 = level 底，看牌跟注 = 2×level 底）。底池 = ${gs.stakes.values.sum()} 分。")
        if (myId in gs.looked) {
            sb.appendLine("你的手牌（已看）：${hand.cards.joinToString(" ") { it.label }}，牌型判定为 ${hand.type.label}（点数 ${hand.tie.joinToString(",")}）。")
        } else {
            sb.appendLine("你还没看牌（闷牌中），不知道手牌。")
        }
        sb.appendLine("其他玩家：")
        players.filter { it.id != myId }.forEach { p ->
            val status = when {
                p.id in gs.folded -> "已弃牌"
                p.id in gs.looked -> "已看牌"
                else -> "闷牌中"
            }
            sb.appendLine("- ${p.name}：$status，已投 ${gs.stakes[p.id] ?: 0} 分")
        }
        gs.lastAction?.let { sb.appendLine("最近动作：$it") }
        sb.appendLine("请只输出 JSON：{\"action\":\"look|call|raise|fold|compare\",\"level\":数字(仅raise),\"target\":玩家ID(仅compare),\"reason\":\"一句话理由\"}")
        return sb.toString()
    }

    private fun buildQuickPrompt(hand: ZjhHand, gs: ZjhQuickSnapshot): String {
        val sb = StringBuilder()
        sb.appendLine("这是炸金花快局：每人都固定下注，闷牌 = 1 底，看牌 = 2 底，全部选完自动亮牌比大小。")
        sb.appendLine("你的手牌：${hand.cards.joinToString(" ") { it.label }}，牌型 ${hand.type.label}（点数 ${hand.tie.joinToString(",")}）。")
        sb.appendLine("其他玩家：")
        gs.others.forEach { (name, choseLook) ->
            sb.appendLine("- $name：${if (choseLook == null) "还没选" else if (choseLook) "选了看牌" else "选了闷牌"}")
        }
        sb.appendLine("请只输出 JSON：{\"look\":true或false,\"reason\":\"一句话理由\"}（true=看牌2底，false=闷牌1底）")
        return sb.toString()
    }

    companion object {
        const val SYSTEM_PROMPT =
            "你是炸金花高手，深谙闷牌/看牌/加注/比牌/弃牌的博弈策略。你根据底池大小、注码水平、对手状态和手牌强度做最理性的决策，必要时虚张声势。"

        /** 快局其他玩家选择快照 */
        data class ZjhQuickSnapshot(
            val others: List<Pair<String, Boolean?>>,
        )
    }
}
