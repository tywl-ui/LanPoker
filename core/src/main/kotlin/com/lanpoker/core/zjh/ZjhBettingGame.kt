package com.lanpoker.core.zjh

import com.lanpoker.core.deck.Card
import com.lanpoker.core.ledger.Player

/**
 * 炸金花下注引擎（闷/看/跟/加/比/弃）。
 *
 * 规则：
 * - 底注 = base（底分单位）。闷牌跟注 = level×base；看牌跟注 = 2×level×base
 * - 加注：把 level 提到新值（闷牌按新 level 记，看牌按 2×新 level 记）
 * - 比牌：发起者先付比牌费（闷 1×/看 2× 当前 level），双方按牌型比大小，输者弃牌；
 *   牌型平局时发起者输（约定俗成）
 * - 只剩一人时本局结束，赢家收走全部底池
 * - 每人每轮必须动作：跟注（已跟够时等同"过"）/ 加注 / 比牌 / 弃牌
 */
class ZjhBettingGame(
    val players: List<Player>,
    val hands: List<List<Card>>,
    val base: Int,
    val rules: ZjhRules = ZjhRules(),
    val maxLevel: Int = 10,
) {
    data class State(
        val stakes: Map<Int, Int>,
        val level: Int,
        val looked: Set<Int>,
        val folded: Set<Int>,
        val turn: Int,
        val over: Boolean,
        val winnerId: Int?,
        val winnerLabel: String?,
        val lastAction: String?,
    )

    private val evaluated: Map<Int, ZjhHand> = players.associate { p ->
        p.id to ZjhEvaluator.evaluate(hands[players.indexOf(p)], rules)
    }

    private var _state = State(
        stakes = players.associate { it.id to base },  // 底注：开局每人自动下 1 底
        level = 1,
        looked = emptySet(),
        folded = emptySet(),
        turn = players.first().id,
        over = false,
        winnerId = null,
        winnerLabel = null,
        lastAction = "${players.first().name} 先下注（底注每人 $base 分）",
    )

    val state: State get() = _state

    fun handLabel(id: Int): String = ZjhEvaluator.describe(evaluated.getValue(id))

    fun handOf(id: Int): ZjhHand = evaluated.getValue(id)

    private fun name(id: Int) = players.first { it.id == id }.name
    private fun stakeOf(id: Int) = _state.stakes[id] ?: 0

    /** 该玩家当前应下的注（底分单位）：闷 = level，看 = 2×level */
    private fun requiredBase(id: Int) = if (id in _state.looked) 2 * _state.level else _state.level

    private fun activeCount() = players.count { it.id !in _state.folded }

    fun isMyTurn(id: Int): Boolean = !_state.over && _state.turn == id && id !in _state.folded

    /** 看牌：把自己的牌翻开（不收费） */
    fun look(): Boolean {
        val id = _state.turn
        if (_state.over || id in _state.folded || id in _state.looked) return false
        _state = _state.copy(looked = _state.looked + id, lastAction = "${name(id)} 看牌")
        return true
    }

    /** 跟注（已跟够等同"过"，转到下家） */
    fun call(): Boolean {
        val id = _state.turn
        if (_state.over || id in _state.folded) return false
        val req = requiredBase(id) * base
        if (stakeOf(id) < req) {
            _state = _state.copy(
                stakes = _state.stakes + (id to req),
                lastAction = "${name(id)} 跟注 $req",
            )
        } else {
            _state = _state.copy(lastAction = "${name(id)} 过")
        }
        advance()
        return true
    }

    /** 加注：把 level 提到 newLevel（必须大于当前 level） */
    fun raise(newLevel: Int): Boolean {
        val id = _state.turn
        if (_state.over || id in _state.folded) return false
        if (newLevel <= _state.level || newLevel > maxLevel) return false
        val stake = newLevel * (if (id in _state.looked) 2 else 1) * base
        _state = _state.copy(
            level = newLevel,
            stakes = _state.stakes + (id to stake),
            lastAction = "${name(id)} 加注到 $newLevel 倍",
        )
        advance()
        return true
    }

    /** 弃牌 */
    fun fold(): Boolean {
        val id = _state.turn
        if (_state.over || id in _state.folded) return false
        _state = _state.copy(
            folded = _state.folded + id,
            lastAction = "${name(id)} 弃牌",
        )
        if (activeCount() == 1) finish() else advance()
        return true
    }

    /**
     * 比牌：与 target 比大小，输者弃牌；平局发起者输。
     * 规则：三家以上时只能和【已看牌】的玩家比牌，不能和闷牌的比；
     * 仅剩两家时，看牌/闷牌双方可以互相开牌。
     */
    fun compare(targetId: Int): Boolean {
        val id = _state.turn
        if (_state.over || id in _state.folded || targetId == id || targetId in _state.folded) return false
        if (activeCount() > 2 && targetId !in _state.looked) return false
        val fee = requiredBase(id) * base
        val challengerLoses = ZjhEvaluator.compare(evaluated.getValue(id), evaluated.getValue(targetId)) <= 0
        val loser = if (challengerLoses) id else targetId
        val newStakes = _state.stakes + (id to stakeOf(id) + fee)
        _state = _state.copy(
            stakes = newStakes,
            folded = _state.folded + loser,
            lastAction = "${name(id)} 与 ${name(targetId)} 比牌，${name(loser)} 输（${handLabel(loser)}）",
        )
        if (activeCount() == 1) finish() else advance()
        return true
    }

    fun pot(): Int = _state.stakes.values.sum()

    /** 结算：赢家 +（底池 - 自己的投入），输家 -各自投入（零和） */
    fun settlementDeltas(): Map<Int, Int> {
        val w = _state.winnerId ?: error("对局未结束")
        val p = pot()
        return players.associate { pl ->
            if (pl.id == w) pl.id to (p - stakeOf(pl.id)) else pl.id to -stakeOf(pl.id)
        }
    }

    private fun advance() {
        val cur = players.indexOfFirst { it.id == _state.turn }
        var i = (cur + 1) % players.size
        while (i != cur && players[i].id in _state.folded) {
            i = (i + 1) % players.size
        }
        _state = _state.copy(turn = players[i].id)
    }

    private fun finish() {
        val winner = players.first { it.id !in _state.folded }
        _state = _state.copy(
            over = true,
            winnerId = winner.id,
            winnerLabel = ZjhEvaluator.describe(evaluated.getValue(winner.id)),
            lastAction = "${winner.name} 赢下全部底池",
        )
    }
}
