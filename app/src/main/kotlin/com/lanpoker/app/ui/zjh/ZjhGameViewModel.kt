package com.lanpoker.app.ui.zjh

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lanpoker.app.ai.AiEngine
import com.lanpoker.core.ai.AiActionType
import com.lanpoker.core.ai.AiDecision
import com.lanpoker.core.config.GameConfig
import com.lanpoker.core.deck.Deck
import com.lanpoker.core.ledger.LedgerSession
import com.lanpoker.core.ledger.Player
import com.lanpoker.core.ledger.RoundResult
import com.lanpoker.core.zjh.ZjhBettingGame
import com.lanpoker.core.zjh.ZjhRules
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Phase { BETTING, SETTLED }

data class UiState(
    val phase: Phase,
    val game: ZjhBettingGame,
    val showMyCards: Boolean,
    val lastResult: RoundResult?,
    val scores: Map<Int, Int>,
    val round: Int,
    val aiThinking: Boolean,
)

class ZjhGameViewModel(
    val config: GameConfig,
    val aiIds: Set<Int>,
    private val aiEngine: AiEngine?,
    names: List<String>,
    private val rules: ZjhRules,
) : ViewModel() {

    val players: List<Player> = (1..config.playerCount).map { i ->
        val raw = names.getOrNull(i - 1)?.trim().orEmpty()
        Player(i, raw.ifBlank { if (i in aiIds) "AI$i" else "玩家$i" })
    }
    private val ledger = LedgerSession(config, players)

    var state by mutableStateOf(newRoundState())
        private set

    init {
        // 首回合可能是 AI：进场立即驱动 AI 行动，否则人机局会卡死
        runAiIfNeeded()
    }

    private fun newRoundState(): UiState {
        val deck = Deck.build(config.deckCount, config.jokerCount)
        val (hands, _) = deck.deal(handSize = 3, playerCount = players.size)
        return UiState(
            phase = Phase.BETTING,
            game = ZjhBettingGame(players, hands, config.baseScore, rules),
            showMyCards = false,
            lastResult = null,
            scores = ledger.scores,
            round = ledger.rounds.size + 1,
            aiThinking = false,
        )
    }

    fun look() {
        if (state.game.look()) state = state.copy(showMyCards = true)
    }

    fun hideCards() {
        state = state.copy(showMyCards = false)
    }

    fun call() {
        if (state.game.call()) afterAction()
    }

    fun raise(level: Int) {
        if (state.game.raise(level)) afterAction()
    }

    fun fold() {
        if (state.game.fold()) afterAction()
    }

    fun compare(targetId: Int) {
        if (state.game.compare(targetId)) afterAction()
    }

    private fun afterAction() {
        val g = state.game
        if (g.state.over) {
            val result = ledger.settleRound(
                winnerId = g.state.winnerId,
                winnerHandLabel = g.state.winnerLabel,
                deltas = g.settlementDeltas(),
            )
            state = state.copy(
                phase = Phase.SETTLED,
                showMyCards = false,
                lastResult = result,
                scores = ledger.scores,
                aiThinking = false,
            )
        } else {
            state = state.copy(showMyCards = false)
            runAiIfNeeded()
        }
    }

    /** 轮到 AI 时自动决策（AI 连打直到轮到真人或结束） */
    private fun runAiIfNeeded() {
        val gs = state.game.state
        if (gs.over || gs.turn !in aiIds) return
        val engine = aiEngine ?: return
        viewModelScope.launch {
            state = state.copy(aiThinking = true)
            while (true) {
                val s = state.game.state
                if (s.over) break
                if (s.turn !in aiIds) break
                delay(1100)
                val id = s.turn
                val hand = state.game.handOf(id)
                val decision = engine.decideBetting(
                    hand = hand,
                    gs = s,
                    myId = id,
                    players = players,
                    base = config.baseScore,
                    maxLevel = state.game.maxLevel,
                )
                val applied = applyDecision(id, decision)
                if (!applied) {
                    // 非法决策（比如重复看牌）→ 兜底：跟注或弃牌
                    if (id in state.game.state.looked) state.game.call() else state.game.look()
                }
            }
            state = state.copy(aiThinking = false)
            val g = state.game
            if (g.state.over) {
                val result = ledger.settleRound(
                    winnerId = g.state.winnerId,
                    winnerHandLabel = g.state.winnerLabel,
                    deltas = g.settlementDeltas(),
                )
                state = state.copy(
                    phase = Phase.SETTLED,
                    lastResult = result,
                    scores = ledger.scores,
                    aiThinking = false,
                )
            }
        }
    }

    private fun applyDecision(id: Int, d: AiDecision): Boolean {
        val g = state.game
        if (g.state.turn != id || g.state.over || id in g.state.folded) return false
        return when (d.action) {
            AiActionType.LOOK -> g.look()
            AiActionType.CALL -> g.call()
            AiActionType.RAISE -> d.level?.let { g.raise(it) } ?: false
            AiActionType.FOLD -> g.fold()
            AiActionType.COMPARE -> d.targetId?.let { g.compare(it) } ?: false
        }
    }

    fun nextRound() {
        state = newRoundState()
        runAiIfNeeded()
    }

    fun exportBill(): String = ledger.exportText()

    companion object {
        fun factory(config: GameConfig, aiIds: Set<Int>, aiEngine: AiEngine?, names: List<String>, rules: ZjhRules) =
            viewModelFactory {
                initializer { ZjhGameViewModel(config, aiIds, aiEngine, names, rules) }
            }
    }
}
