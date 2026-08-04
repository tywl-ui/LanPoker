package com.lanpoker.app.ui.zjh

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lanpoker.core.config.GameConfig
import com.lanpoker.core.deck.Card
import com.lanpoker.core.deck.Deck
import com.lanpoker.core.ledger.LedgerSession
import com.lanpoker.core.ledger.Player
import com.lanpoker.core.ledger.RoundResult
import com.lanpoker.core.zjh.ZjhEvaluator
import com.lanpoker.core.zjh.ZjhHand
import com.lanpoker.core.zjh.TieRule

enum class Phase { PASS_PHONE, REVEAL, MULTIPLIER, SETTLED }

data class UiState(
    val phase: Phase,
    val hands: List<List<Card>>,
    val evaluated: List<ZjhHand>,
    val currentViewer: Int,
    val winnerIndex: Int?,
    val multipliers: Map<Int, Int>,
    val lastResult: RoundResult?,
    val scores: Map<Int, Int>,
    val message: String?,
)

class ZjhGameViewModel(
    val config: GameConfig,
) : ViewModel() {

    val players: List<Player> = (1..config.playerCount).map { Player(it, "玩家$it") }
    private val ledger = LedgerSession(config, players)

    var state by mutableStateOf(UiState(
        phase = Phase.PASS_PHONE,
        hands = emptyList(),
        evaluated = emptyList(),
        currentViewer = 0,
        winnerIndex = null,
        multipliers = emptyMap(),
        lastResult = null,
        scores = ledger.scores,
        message = null,
    ))
        private set

    init {
        deal()
    }

    /** 发牌，开始传手机看牌 */
    fun deal() {
        val deck = Deck.build(config.deckCount, config.jokerCount)
        val (hands, _) = deck.deal(handSize = 3, playerCount = config.playerCount)
        state = state.copy(
            phase = Phase.PASS_PHONE,
            hands = hands,
            evaluated = emptyList(),
            currentViewer = 0,
            winnerIndex = null,
            multipliers = emptyMap(),
            message = null,
        )
    }

    /** 当前玩家看完牌，传给下一位；最后一位看完后亮牌判定 */
    fun confirmViewed() {
        if (state.currentViewer < state.hands.size - 1) {
            state = state.copy(currentViewer = state.currentViewer + 1)
            return
        }
        val evaluated = state.hands.map { ZjhEvaluator.evaluate(it) }
        val winner = ZjhEvaluator.strongestIndex(evaluated, TieRule.REDEAL)
        if (winner == null) {
            // 和局：重发一局
            val deck = Deck.build(config.deckCount, config.jokerCount)
            val (hands, _) = deck.deal(handSize = 3, playerCount = config.playerCount)
            state = state.copy(
                phase = Phase.PASS_PHONE,
                hands = hands,
                evaluated = emptyList(),
                currentViewer = 0,
                message = "本局和局，自动重发一局",
            )
        } else {
            state = state.copy(
                phase = Phase.REVEAL,
                evaluated = evaluated,
                winnerIndex = winner,
            )
        }
    }

    /** 进入填倍数阶段，输家默认填 1 */
    fun startMultiplier() {
        val w = state.winnerIndex ?: return
        val losers = players.filterIndexed { i, _ -> i != w }
        state = state.copy(
            phase = Phase.MULTIPLIER,
            multipliers = losers.associate { it.id to 1 },
        )
    }

    fun setMultiplier(playerId: Int, value: Int) {
        state = state.copy(
            multipliers = state.multipliers + (playerId to value.coerceAtLeast(0)),
        )
    }

    /** 结算本局并记账 */
    fun settle() {
        val w = state.winnerIndex ?: return
        val winner = players[w]
        val handLabel = ZjhEvaluator.describe(state.evaluated[w])
        val result = ledger.settle(
            winnerId = winner.id,
            winnerHandLabel = handLabel,
            multipliers = state.multipliers,
        )
        state = state.copy(
            phase = Phase.SETTLED,
            lastResult = result,
            scores = ledger.scores,
        )
    }

    fun exportBill(): String = ledger.exportText()

    companion object {
        fun factory(config: GameConfig) = viewModelFactory {
            initializer { ZjhGameViewModel(config) }
        }
    }
}
