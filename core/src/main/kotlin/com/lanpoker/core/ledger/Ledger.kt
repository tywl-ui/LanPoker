package com.lanpoker.core.ledger

import com.lanpoker.core.config.GameConfig

data class Player(val id: Int, val name: String)

/** 一局里某个玩家的账目行 */
data class Entry(
    val playerId: Int,
    val playerName: String,
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
 * 结算按每局实际分数差（deltas）累计；deltas 之和恒为 0（赢家收底池，输家付投入）。
 */
class LedgerSession(
    val config: GameConfig,
    val players: List<Player>,
) {
    private val _rounds = mutableListOf<RoundResult>()
    private val _scores = players.associate { it.id to 0 }.toMutableMap()

    val rounds: List<RoundResult> get() = _rounds
    val scores: Map<Int, Int> get() = _scores

    /** 记一局。winnerId 为 null 视为和局（全 0 不计分） */
    fun settleRound(
        winnerId: Int?,
        winnerHandLabel: String?,
        deltas: Map<Int, Int>,
    ): RoundResult {
        val result = RoundResult(
            round = _rounds.size + 1,
            winnerId = winnerId,
            winnerName = players.find { it.id == winnerId }?.name,
            winnerHandLabel = winnerHandLabel,
            entries = players.map { p -> Entry(p.id, p.name, deltas[p.id] ?: 0) },
            deltas = deltas,
        )
        deltas.forEach { (id, d) -> _scores[id] = _scores.getValue(id) + d }
        _rounds += result
        return result
    }

    /** 导出纯文本账单，方便转发 */
    fun exportText(): String {
        val sb = StringBuilder()
        sb.appendLine("【局域网棋牌】记账单")
        sb.appendLine("玩法：${config.gameType.label} | ${config.deckCount} 副牌 | ${players.size} 人 | 底分 ${config.baseScore}")
        sb.appendLine("玩家：" + players.joinToString("、") { it.name })
        sb.appendLine("----")
        for (r in _rounds) {
            if (r.isDraw) {
                sb.appendLine("第 ${r.round} 局：和局，不计分")
            } else {
                sb.appendLine("第 ${r.round} 局：${r.winnerName} 赢（${r.winnerHandLabel}）")
                for (e in r.entries) {
                    if (e.playerId != r.winnerId) {
                        sb.appendLine("  ${e.playerName} ${signed(e.delta)}")
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
