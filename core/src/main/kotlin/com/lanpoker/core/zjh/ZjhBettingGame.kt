package com.lanpoker.core.zjh

import com.lanpoker.core.deck.Card
import com.lanpoker.core.deck.Rank
import com.lanpoker.core.deck.Suit
import com.lanpoker.core.ledger.Player

/**
 * 炸金花下注引擎（闷/看/跟/加/比/弃）。
 *
 * 规则：
 * - 底注 = base（底分单位）。闷牌跟注 = level×base；看牌跟注 = 2×level×base
 * - 加注：把 level 提到新值（闷牌按新 level 记，看牌按 2×新 level 记）
 * - 比牌：双方各付比牌费（闷 1×/看 2× 当前 level）；输者弃牌；平局发起者输
 * - 三家以上只能和【已看牌】的玩家比牌；仅剩两家时可与闷牌者开牌
 * - 王（百搭）在比牌时定型：发起者先自选一个能组成的牌型；对方若有王，
 *   可在「能赢过所选牌型」的范围内再选；选不出则输
 * - 只剩一人时本局结束，赢家收走全部底池
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
        val jokerLock: Map<Int, ZjhHand> = emptyMap(),
        /** 最近一次比牌的双方与胜负（供对决特效展示） */
        val lastCompare: CompareEvent? = null,
    )

    /** 比牌事件：谁和谁比、各自牌型、谁输 */
    data class CompareEvent(
        val challengerId: Int,
        val targetId: Int,
        val challengerLabel: String,
        val targetLabel: String,
        val loserId: Int,
    )

    /** 每位玩家能组成的全部牌型（去重），王玩家为可选项，非王玩家只有最优一项 */
    private val transforms: Map<Int, List<ZjhHand>>
    private val bestEval: Map<Int, ZjhHand>
    private val jokerPlayers: Set<Int>

    init {
        bestEval = players.associate { p ->
            p.id to ZjhEvaluator.evaluate(hands[players.indexOf(p)], rules)
        }
        transforms = players.associate { p ->
            val hand = hands[players.indexOf(p)]
            val all = if (hand.any { it is Card.Joker }) enumerateTransforms(hand)
            else listOf(bestEval.getValue(p.id))
            p.id to dedup(all)
        }
        jokerPlayers = players.filter { p -> hands[players.indexOf(p)].any { it is Card.Joker } }
            .map { it.id }.toSet()
    }

    /** 王的所有可能替代，逐一求值（52^王数 次，开局一次） */
    private fun enumerateTransforms(hand: List<Card>): List<ZjhHand> {
        val jokers = hand.filterIsInstance<Card.Joker>()
        val normals = hand.filterIsInstance<Card.Poker>()
        val variants = Rank.entries.flatMap { r -> Suit.entries.map { s -> Card.Poker(r, s) } }
        val results = mutableListOf<ZjhHand>()
        fun rec(idx: Int, fixed: List<Card>) {
            if (idx == jokers.size) {
                val h = ZjhEvaluator.evaluate(fixed, rules)
                // 1-2 副牌不允许同花豹：王不能变成三张同花色同点数
                if (rules.allowSameSuitTriple || !ZjhEvaluator.isSameSuitTriple(h)) {
                    results += h
                }
                return
            }
            for (v in variants) rec(idx + 1, fixed + v)
        }
        rec(0, normals)
        return results
    }

    /** 按（牌型,点数）去重 */
    private fun dedup(all: List<ZjhHand>): List<ZjhHand> {
        val seen = LinkedHashMap<Pair<ZjhHandType, List<Int>>, ZjhHand>()
        all.forEach { h -> seen.putIfAbsent(h.type to h.tie, h) }
        return seen.values.toList()
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

    fun handLabel(id: Int): String = labelFor(id, null)

    fun handOf(id: Int): ZjhHand = bestEval.getValue(id)

    /** 该玩家全部可选的牌型（王玩家），非王玩家返回最优一项 */
    fun transformsOf(id: Int): List<ZjhHand> = transforms.getValue(id)

    /** 该玩家是否持有王 */
    fun hasJoker(id: Int): Boolean = id in jokerPlayers

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

    /** 比牌结果：RESOLVED 已定胜负；AWAITING_TARGET 需对方再选牌型 */
    enum class CompareStep { RESOLVED, AWAITING_TARGET }

    data class CompareResult(
        val step: CompareStep,
        val loserId: Int? = null,
        val targetOptions: List<ZjhHand> = emptyList(),
    )

    /**
     * 发起比牌（兼容无王发起者，challengerHand 为 null 用最优牌型）。
     * 有王发起者先锁定所选牌型；对方无王立即定胜负，对方有王且能赢则等待其选型。
     */
    fun beginCompare(targetId: Int, challengerHand: ZjhHand? = null): CompareResult? {
        val id = _state.turn
        if (_state.over || id in _state.folded || targetId == id || targetId in _state.folded) return null
        if (activeCount() > 2 && targetId !in _state.looked) return null
        if (challengerHand != null && challengerHand !in transforms.getValue(id)) return null

        val cHand = challengerHand ?: bestEval.getValue(id)
        if (challengerHand != null) {
            _state = _state.copy(jokerLock = _state.jokerLock + (id to cHand))
        }

        // 对方无王：固定牌直接定胜负（平局发起者输）
        if (targetId !in jokerPlayers) {
            val tHand = bestEval.getValue(targetId)
            val loser = if (ZjhEvaluator.compare(cHand, tHand) <= 0) id else targetId
            applyCompareOutcome(
                id, targetId, tHand, loser,
                challengerLabel = ZjhEvaluator.optionLabel(cHand),
                targetLabel = ZjhEvaluator.optionLabel(tHand),
            )
            return CompareResult(CompareStep.RESOLVED, loserId = loser)
        }
        // 对方有王：在「能赢过所选牌型」的范围内选
        val wins = transforms.getValue(targetId).filter { ZjhEvaluator.compare(it, cHand) > 0 }
        if (wins.isEmpty()) {
            val tBest = bestEval.getValue(targetId)
            applyCompareOutcome(
                id, targetId, null, loser = targetId,
                challengerLabel = ZjhEvaluator.optionLabel(cHand),
                targetLabel = ZjhEvaluator.optionLabel(tBest),
            )
            return CompareResult(CompareStep.RESOLVED, loserId = targetId)
        }
        return CompareResult(CompareStep.AWAITING_TARGET, targetOptions = wins)
    }

    /** 对方（有王）选定能赢的牌型，完成比牌 */
    fun finalizeCompare(targetId: Int, targetHand: ZjhHand): Boolean {
        val id = _state.turn
        if (_state.over || id in _state.folded || targetId in _state.folded) return false
        if (targetId !in jokerPlayers || targetHand !in transforms.getValue(targetId)) return false
        val cHand = _state.jokerLock[id] ?: bestEval.getValue(id)
        if (ZjhEvaluator.compare(targetHand, cHand) <= 0) return false
        applyCompareOutcome(
            id, targetId, targetHand, loser = id,
            challengerLabel = ZjhEvaluator.optionLabel(cHand),
            targetLabel = ZjhEvaluator.optionLabel(targetHand),
        )
        return true
    }

    /** 老接口：无王发起者的立即比牌 */
    fun compare(targetId: Int): Boolean {
        val r = beginCompare(targetId, null) ?: return false
        return r.step == CompareStep.RESOLVED
    }

    /** 比牌双方各付比牌费并淘汰输家 */
    private fun applyCompareOutcome(
        id: Int,
        targetId: Int,
        tHand: ZjhHand?,
        loser: Int,
        challengerLabel: String,
        targetLabel: String,
    ) {
        val fee = requiredBase(id) * base
        val feeTarget = requiredBase(targetId) * base
        // 只有王玩家才锁定选定的牌型
        val lock = if (tHand != null && targetId in jokerPlayers) {
            _state.jokerLock + (targetId to tHand)
        } else {
            _state.jokerLock
        }
        _state = _state.copy(
            stakes = _state.stakes
                .plus(id to stakeOf(id) + fee)
                .plus(targetId to stakeOf(targetId) + feeTarget),
            folded = _state.folded + loser,
            jokerLock = lock,
            lastCompare = CompareEvent(id, targetId, challengerLabel, targetLabel, loser),
            // 日志只报谁输，不亮出双方牌面（牌面由结算页展示赢家）
            lastAction = "${name(id)} 与 ${name(targetId)} 比牌，${name(loser)} 输",
        )
        if (activeCount() == 1) finish() else advance()
    }

    /** 对决特效展示完毕后清除比牌事件 */
    fun clearCompareEvent() {
        _state = _state.copy(lastCompare = null)
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

    /** 牌型描述：真实手牌 + 选定的牌型（王玩家用已定型/最优） */
    private fun labelFor(id: Int, hand: ZjhHand?): String {
        val h = hand ?: _state.jokerLock[id] ?: bestEval.getValue(id)
        val realCards = hands[players.indexOfFirst { it.id == id }].joinToString("") { it.label }
        return "${h.type.label}$realCards"
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
            winnerLabel = labelFor(winner.id, _state.jokerLock[winner.id]),
            lastAction = "${winner.name} 赢下全部底池",
        )
    }
}
