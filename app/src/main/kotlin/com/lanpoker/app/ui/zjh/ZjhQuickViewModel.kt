package com.lanpoker.app.ui.zjh

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lanpoker.app.ai.AiEngine
import com.lanpoker.core.config.GameConfig
import com.lanpoker.core.deck.Deck
import com.lanpoker.core.ledger.LedgerSession
import com.lanpoker.core.ledger.Player
import com.lanpoker.core.ledger.RoundResult
import com.lanpoker.core.zjh.ZjhEvaluator
import com.lanpoker.core.zjh.ZjhQuickGame
import com.lanpoker.core.zjh.ZjhRules
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class QuickPhase { CHOOSE, REVEAL }

data class QuickUiState(
    val phase: QuickPhase,
    val game: ZjhQuickGame,
    val showMyCards: Boolean,
    val lastResult: RoundResult?,
    val scores: Map<Int, Int>,
    val round: Int,
    val aiThinking: Boolean,
)

/**
 * 快局：固定倍数（闷 1 底 / 看 2 底），连开多把。
 * 真人轮流选闷/看（共用一台手机），AI 自动选择，全部选定自动亮牌结算。
 */
class ZjhQuickViewModel(
    val config: GameConfig,
    val aiIds: Set<Int>,
    private val aiEngine: AiEngine,
) : ViewModel() {

    val players: List<Player> = (1..config.playerCount).map { Player(it, if (it in aiIds) "AI$it" else "玩家$it") }
    private val ledger = LedgerSession(config, players)

    var state by mutableStateOf(newRound())
        private set

    private fun newRound(): QuickUiState {
        val deck = Deck.build(config.deckCount, config.jokerCount)
        val (hands, _) = deck.deal(handSize = 3, playerCount = players.size)
        return QuickUiState(
            phase = QuickPhase.CHOOSE,
            game = ZjhQuickGame(players, hands, config.baseScore, ZjhRules()),
            showMyCards = false,
            lastResult = null,
            scores = ledger.scores,
            round = ledger.rounds.size + 1,
            aiThinking = false,
        )
    }

    /** 真人选择：闷/看（看牌先展示自己的牌 1.8 秒，再真正落注并轮到下家） */
    fun choose(looked: Boolean) {
        val chooser = state.game.currentChooser ?: return
        if (chooser.id in aiIds) return
        if (looked) {
            state = state.copy(showMyCards = true)
            viewModelScope.launch {
                delay(1800)
                state = state.copy(showMyCards = false)
                if (state.game.allChosen) return@launch
                if (!state.game.choose(chooser.id, true)) return@launch
                afterChoose()
            }
        } else {
            if (!state.game.choose(chooser.id, false)) return
            afterChoose()
        }
    }

    private fun afterChoose() {
        val g = state.game
        if (g.allChosen) {
            settle()
        } else {
            runAiIfNeeded()
        }
    }

    private fun runAiIfNeeded() {
        val chooser = state.game.currentChooser ?: return
        if (chooser.id !in aiIds) return
        viewModelScope.launch {
            state = state.copy(aiThinking = true)
            while (true) {
                val c = state.game.currentChooser ?: break
                if (c.id !in aiIds) break
                if (state.game.allChosen) break
                delay(1000)
                val hand = ZjhEvaluator.evaluate(state.game.hands[players.indexOf(c)])
                val snapshot = AiEngine.Companion.ZjhQuickSnapshot(
                    others = players.filter { it.id != c.id }.map { p ->
                        p.name to state.game.state.chosen[p.id]?.let { it != config.baseScore }
                    },
                )
                val look = aiEngine.decideQuickLook(hand, snapshot)
                if (!state.game.choose(c.id, look)) break
            }
            state = state.copy(aiThinking = false)
            if (state.game.allChosen) settle()
        }
    }

    private fun settle() {
        val g = state.game
        val result = if (g.isDraw) {
            ledger.settleRound(winnerId = null, winnerHandLabel = null, deltas = g.settlementDeltas())
        } else {
            ledger.settleRound(
                winnerId = g.state.winnerId,
                winnerHandLabel = g.state.winnerLabel,
                deltas = g.settlementDeltas(),
            )
        }
        state = state.copy(
            phase = QuickPhase.REVEAL,
            lastResult = result,
            scores = ledger.scores,
            aiThinking = false,
        )
    }

    fun nextRound() {
        state = newRound()
        runAiIfNeeded()
    }

    fun exportBill(): String = ledger.exportText()

    companion object {
        fun factory(config: GameConfig, aiIds: Set<Int>, aiEngine: AiEngine) = viewModelFactory {
            initializer { ZjhQuickViewModel(config, aiIds, aiEngine) }
        }
    }
}
