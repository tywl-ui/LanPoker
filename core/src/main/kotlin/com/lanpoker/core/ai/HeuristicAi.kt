package com.lanpoker.core.ai

import com.lanpoker.core.zjh.ZjhBettingGame
import com.lanpoker.core.zjh.ZjhHand
import com.lanpoker.core.zjh.ZjhHandType
import kotlin.random.Random

/**
 * 内置启发式 AI：不依赖网络，按手牌强度 + 底池赔率做决策。
 * 强度分：豹子/235 100 档，顺金 90 档，金花 70 档，顺子 60 档，对子 40 档，单张 20 档。
 */
object HeuristicAi {

    fun strength(hand: ZjhHand): Int = when (hand.type) {
        ZjhHandType.TRIPLE -> 100
        ZjhHandType.SPECIAL_235 -> 98
        ZjhHandType.STRAIGHT_FLUSH -> 90
        ZjhHandType.FLUSH -> 70
        ZjhHandType.STRAIGHT -> 60
        ZjhHandType.PAIR -> 40 + hand.tie[0] / 4
        ZjhHandType.HIGH_CARD -> 18 + hand.tie[0] / 4
    }

    /** 标准局决策 */
    fun decideBetting(
        hand: ZjhHand,
        gs: ZjhBettingGame.State,
        myId: Int,
        base: Int,
        maxLevel: Int,
        rng: Random = Random.Default,
    ): AiDecision {
        // 还没看牌：先看（强牌可玩闷战术，小概率继续闷）
        if (myId !in gs.looked) {
            if (strength(hand) >= 85 && rng.nextInt(100) < 30) {
                return AiDecision(AiActionType.CALL, reason = "手气好，先闷一把")
            }
            return AiDecision(AiActionType.LOOK, reason = "看牌")
        }

        val s = strength(hand)
        val stake = gs.stakes[myId] ?: 0
        val req = (if (myId in gs.looked) 2 else 1) * gs.level * base
        val needPay = req - stake

        return when {
            s >= 90 && gs.level < maxLevel ->
                AiDecision(AiActionType.RAISE, level = (gs.level + if (s >= 100) 2 else 1).coerceAtMost(maxLevel), reason = "牌很强，加注")
            s >= 85 && gs.level < maxLevel && rng.nextInt(100) < 40 ->
                AiDecision(AiActionType.RAISE, level = gs.level + 1, reason = "加注试探")
            s >= 60 -> AiDecision(AiActionType.CALL, reason = "牌不错，跟注")
            s >= 40 -> {
                if (needPay > 3 * base) AiDecision(AiActionType.FOLD, reason = "跟注太贵，弃牌")
                else AiDecision(AiActionType.CALL, reason = "小注跟一手")
            }
            else -> {
                when {
                    needPay > 2 * base -> AiDecision(AiActionType.FOLD, reason = "牌弱，弃牌")
                    needPay == 0 && rng.nextInt(100) < 60 -> AiDecision(AiActionType.FOLD, reason = "牌太弱")
                    rng.nextInt(100) < 25 -> AiDecision(AiActionType.CALL, reason = "虚张声势，跟一手")
                    else -> AiDecision(AiActionType.FOLD, reason = "牌弱，弃牌")
                }
            }
        }
    }

    /** 快局（固定倍数）闷/看选择：强牌看牌，弱牌闷牌省钱，随机一点变化 */
    fun decideQuickLook(strengthScore: Int, rng: Random = Random.Default): Boolean {
        if (strengthScore >= 70) return true
        if (strengthScore >= 50) return rng.nextBoolean()
        return rng.nextInt(100) < 40
    }
}
