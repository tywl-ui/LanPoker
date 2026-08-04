package com.lanpoker.core.zjh

import com.lanpoker.core.deck.Card
import com.lanpoker.core.ledger.Player

/**
 * 炸金花快局（人机连开模式）：
 * 每人固定下注（闷 = 1 底，看牌 = 2 底），全部选定后自动亮牌比大小，赢家收底池。
 * 适合快速连续玩多把。
 */
class ZjhQuickGame(
    val players: List<Player>,
    val hands: List<List<Card>>,
    val base: Int,
    val rules: ZjhRules = ZjhRules(),
) {
    data class State(
        val chosen: Map<Int, Int>,   // playerId -> 投入(底分)
        val over: Boolean,
        val winnerId: Int?,
        val winnerLabel: String?,
    )

    private val evaluated = players.associate { p ->
        p.id to ZjhEvaluator.evaluate(hands[players.indexOf(p)], rules)
    }

    private var _state = State(chosen = emptyMap(), over = false, winnerId = null, winnerLabel = null)
    val state: State get() = _state

    fun handLabel(id: Int): String = ZjhEvaluator.describe(evaluated.getValue(id))

    val currentChooser: Player?
        get() = players.firstOrNull { it.id !in _state.chosen }

    fun choose(id: Int, looked: Boolean): Boolean {
        if (_state.over || id in _state.chosen) return false
        val stake = if (looked) 2 * base else base
        _state = _state.copy(chosen = _state.chosen + (id to stake))
        if (_state.chosen.size == players.size) finish()
        return true
    }

    /** 是否全部选定（亮牌） */
    val allChosen: Boolean get() = _state.chosen.size == players.size

    /** 和局（牌型完全平手） */
    val isDraw: Boolean get() = _state.over && _state.winnerId == null

    private fun finish() {
        val handsEval = players.map { evaluated.getValue(it.id) }
        val winner = ZjhEvaluator.strongestIndex(handsEval, TieRule.REDEAL)
        _state = if (winner == null) {
            _state.copy(over = true)
        } else {
            val w = players[winner]
            _state.copy(over = true, winnerId = w.id, winnerLabel = ZjhEvaluator.describe(handsEval[winner]))
        }
    }

    /** 零和结算：赢家 +Σ(别人投入)，输家 -自己投入；和局全 0 */
    fun settlementDeltas(): Map<Int, Int> {
        if (isDraw) return players.associate { it.id to 0 }
        val w = _state.winnerId ?: return players.associate { it.id to 0 }
        val othersSum = _state.chosen.filterKeys { it != w }.values.sum()
        return players.associate { p ->
            if (p.id == w) p.id to othersSum else p.id to -(_state.chosen[p.id] ?: 0)
        }
    }
}
