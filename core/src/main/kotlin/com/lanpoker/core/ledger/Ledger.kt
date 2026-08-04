package com.lanpoker.core.ledger

import com.lanpoker.core.config.GameConfig

data class Player(val id: Int, val name: String)

/** 一局里某个玩家的账目行 */
data class Entry(
    val playerId: Int,
    val playerName: String,
    val multiplier: Int,
    val delta: Int,
)

/** 一局的结果记录 */
data class RoundResult(
    val round: Int,
    val winnerId: Int?,
    val winnerName: String?,
    val winnerHandLabel: String?,
    val entries: List<Entry>,
    val deltas: Map<Int, Int>,
) {
    val isDraw: Boolean get() = winnerId == null
}

/**
 * 记账会话：记录一桌人若干局的输赢。
 * 结算规则（炸金花模式 A）：赢家收入 = Σ(输家填的倍数 × 底分)；每个输家扣 自身倍数 × 底分。
 * winnerId == null 视为和局/退钱，本局不记分。
 */
class LedgerSession(
    val config: GameConfig,
    val players: List<Player>,
) {
    private val _rounds = mutableListOf<RoundResult>()
    private val _scores = players.associate { it.id to 0 }.toMutableMap()

    val rounds: List<RoundResult> get() = _rounds
    val scores: Map<Int, Int> get() = _scores

    /** @param multipliers 玩家自填倍数（输家填自己输几个底；赢家/和局可不填） */
    fun settle(
        winnerId: Int?,
        winnerHandLabel: String?,
        multipliers: Map<Int, Int>,
    ): RoundResult {
        val deltas = mutableMapOf<Int, Int>()
        for (p in players) {
            deltas[p.id] = if (winnerId == null) 0
            else if (p.id == winnerId) {
                players.filter { it.id != winnerId }
                    .sumOf { (multipliers[it.id] ?: 0) * config.baseScore }
            } else {
                -(multipliers[p.id] ?: 0) * config.baseScore
            }
        }
        deltas.forEach { (id, d) -> _scores[id] = _scores.getValue(id) + d }
        val result = RoundResult(
            round = _rounds.size + 1,
            winnerId = winnerId,
            winnerName = players.find { it.id == winnerId }?.name,
            winnerHandLabel = winnerHandLabel,
            entries = players.map { p ->
                Entry(p.id, p.name, multipliers[p.id] ?: 0, deltas.getValue(p.id))
            },
            deltas = deltas,
        )
        _rounds += result
        return result
    }

    /** 导出纯文本账单，方便转发 */
    fun exportText(): String {
        val sb = StringBuilder()
        sb.appendLine("【局域网棋牌】记账单")
        sb.appendLine("玩法：${config.gameType.label} | ${config.deckCount} 副牌 | ${players.size} 人 | 底分 ${config.baseScore}")
        sb.appendLine("玩家：" + players.joinToString("、") { "${it.name}" })
        sb.appendLine("----")
        for (r in _rounds) {
            if (r.isDraw) {
                sb.appendLine("第 ${r.round} 局：和局，不计分")
            } else {
                sb.appendLine("第 ${r.round} 局：${r.winnerName} 赢（${r.winnerHandLabel}）")
                for (e in r.entries) {
                    if (e.playerId != r.winnerId) {
                        sb.appendLine("  ${e.playerName} 填 ${e.multiplier} 底，${signed(e.delta)}")
                    }
                }
            }
        }
        sb.appendLine("----")
        sb.appendLine("总分：" + _scores.entries.joinToString("、") { (id, v) ->
            "${players.first { it.id == id }.name} ${signed(v)}"
        })
        return sb.toString()
    }

    private fun signed(v: Int) = if (v >= 0) "+$v" else "$v"
}
