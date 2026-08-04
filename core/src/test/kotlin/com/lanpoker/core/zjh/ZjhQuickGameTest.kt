package com.lanpoker.core.zjh

import com.lanpoker.core.deck.Card
import com.lanpoker.core.deck.Rank
import com.lanpoker.core.deck.Suit
import com.lanpoker.core.ledger.Player
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ZjhQuickGameTest {

    private fun p(rank: Rank, suit: Suit) = Card.Poker(rank, suit)

    private fun game(hands: List<List<Card>>) = ZjhQuickGame(
        players = listOf(Player(1, "甲"), Player(2, "乙"), Player(3, "丙")),
        hands = hands,
        base = 5,
    )

    private val strongHands = listOf(
        listOf(p(Rank.ACE, Suit.SPADE), p(Rank.ACE, Suit.HEART), p(Rank.ACE, Suit.CLUB)), // 豹子
        listOf(p(Rank.KING, Suit.CLUB), p(Rank.KING, Suit.DIAMOND), p(Rank.NINE, Suit.SPADE)), // 对K
        listOf(p(Rank.THREE, Suit.SPADE), p(Rank.FOUR, Suit.HEART), p(Rank.FIVE, Suit.CLUB)), // 顺子
    )

    @Test
    fun 每人选择后自动开牌() {
        val g = game(strongHands)
        assertEquals(1, g.currentChooser?.id)
        assertTrue(g.choose(1, looked = true))   // 甲看牌 2底
        assertEquals(2, g.currentChooser?.id)
        assertTrue(g.choose(2, looked = false))  // 乙闷 1底
        assertTrue(g.choose(3, looked = false))  // 丙闷 1底
        assertTrue(g.state.over)
        assertEquals(1, g.state.winnerId)        // 豹子赢
        assertEquals(mapOf(1 to 10, 2 to -5, 3 to -5), g.settlementDeltas())
    }

    @Test
    fun 重复选择无效() {
        val g = game(strongHands)
        g.choose(1, true)
        assertFalse(g.choose(1, false))
    }

    @Test
    fun 和局不记分() {
        val hands = listOf(
            listOf(p(Rank.KING, Suit.SPADE), p(Rank.KING, Suit.HEART), p(Rank.NINE, Suit.CLUB)),
            listOf(p(Rank.KING, Suit.CLUB), p(Rank.KING, Suit.DIAMOND), p(Rank.NINE, Suit.SPADE)),
            listOf(p(Rank.ACE, Suit.SPADE), p(Rank.KING, Suit.HEART), p(Rank.NINE, Suit.CLUB)),
        )
        val g = game(hands)
        g.choose(1, true)
        g.choose(2, false)
        g.choose(3, false)
        assertTrue(g.isDraw)
        assertEquals(mapOf(1 to 0, 2 to 0, 3 to 0), g.settlementDeltas())
    }

    @Test
    fun 零和() {
        val g = game(strongHands)
        g.choose(1, true)
        g.choose(2, true)
        g.choose(3, true)
        val deltas = g.settlementDeltas()
        assertEquals(0, deltas.values.sum())
        assertEquals(mapOf(1 to 20, 2 to -10, 3 to -10), deltas)
    }
}
