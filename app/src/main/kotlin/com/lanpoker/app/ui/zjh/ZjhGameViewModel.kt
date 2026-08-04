package com.lanpoker.app.ui.zjh

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lanpoker.core.config.GameConfig
import com.lanpoker.core.deck.Deck
import com.lanpoker.core.ledger.LedgerSession
import com.lanpoker.core.ledger.Player
import com.lanpoker.core.ledger.RoundResult
import com.lanpoker.core.zjh.ZjhBettingGame
import com.lanpoker.core.zjh.ZjhRules

enum class Phase { BETTING, SETTLED }

data class UiState(
    val phase: Phase,
    val game: ZjhBettingGame,
    val showMyCards: Boolean,
    val lastResult: RoundResult?,
    val scores: Map<Int, Int>,
    val round: Int,
)

class ZjhGameViewModel(
    val config: GameConfig,
) : ViewModel() {

    val players: List<Player> = (1..config.playerCount).map { Player(it, "玩家$it") }
    private val ledger = LedgerSession(config, players)

    var state by mutableStateOf(newRoundState())
        private set

    private fun newRoundState(): UiState {
        val deck = Deck.build(config.deckCount, config.jokerCount)
        val (hands, _) = deck.deal(handSize = 3, playerCount = players.size)
        return UiState(
            phase = Phase.BETTING,
            game = ZjhBettingGame(players, hands, config.baseScore, ZjhRules()),
            showMyCards = false,
            lastResult = null,
            scores = ledger.scores,
            round = ledger.rounds.size + 1,
        )
    }

    /** 当前行动者翻看自己的牌（视觉上展示，随后自动合上） */
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
            )
        } else {
            state = state.copy(showMyCards = false)
        }
    }

    fun nextRound() {
        state = newRoundState()
    }

    fun exportBill(): String = ledger.exportText()

    companion object {
        fun factory(config: GameConfig) = viewModelFactory {
            initializer { ZjhGameViewModel(config) }
        }
    }
}
