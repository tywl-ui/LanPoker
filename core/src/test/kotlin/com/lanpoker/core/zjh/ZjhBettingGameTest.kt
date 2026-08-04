package com.lanpoker.core.zjh

import com.lanpoker.core.deck.Card
import com.lanpoker.core.deck.Rank
import com.lanpoker.core.deck.Suit
import com.lanpoker.core.ledger.Player
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ZjhBettingGameTest {

    private fun p(rank: Rank, suit: Suit) = Card.Poker(rank, suit)

    private fun game(
        players: List<Player> = listOf(Player(1, "甲"), Player(2, "乙"), Player(3, "丙")),
        hands: List<List<Card>> = listOf(
            listOf(p(Rank.ACE, Suit.SPADE), p(Rank.KING, Suit.HEART), p(Rank.NINE, Suit.CLUB)),
            listOf(p(Rank.KING, Suit.SPADE), p(Rank.KING, Suit.HEART), p(Rank.NINE, Suit.CLUB)),
            listOf(p(Rank.THREE, Suit.SPADE), p(Rank.FOUR, Suit.HEART), p(Rank.FIVE, Suit.CLUB)),
        ),
        base: Int = 1,
    ) = ZjhBettingGame(players, hands, base)

    @Test
    fun 闷牌跟注下1底() {
        val g = game(base = 2)
        g.call()
        assertEquals(2, g.state.stakes[1])
        assertEquals(2, g.state.turn) // 轮到下家
    }

    @Test
    fun 看牌跟注下2倍() {
        val g = game()
        g.look()          // 玩家1看牌
        g.call()          // 看牌跟 = 2×1底
        assertEquals(2, g.state.stakes[1])
        assertEquals(setOf(1), g.state.looked)
    }

    @Test
    fun 看牌玩家加注按2倍() {
        val g = game()
        g.look()
        g.raise(2)
        assertEquals(4, g.state.stakes[1])
    }

    @Test
    fun 加注不能低于或等于当前level() {
        val g = game()
        assertFalse(g.raise(1))
        g.raise(3)
        assertFalse(g.raise(3))
        assertFalse(g.raise(2))
    }

    @Test
    fun 加注提高level() {
        val g = game()
        g.raise(3)
        assertEquals(3, g.state.level)
        assertEquals(3, g.state.stakes[1]) // 闷牌加到3底
        assertEquals(2, g.state.turn)
    }

    @Test
    fun 跟注不足时补齐() {
        val g = game()
        g.raise(3)                    // 甲加到3
        assertEquals(2, g.state.turn)
        g.call()                      // 乙闷跟3
        assertEquals(3, g.state.stakes[2])
        assertEquals(3, g.state.turn) // 轮到丙
        g.call()                      // 丙闷跟3
        assertEquals(3, g.state.stakes[3])
        assertEquals(1, g.state.turn) // 回到甲
        g.call()                      // 甲已跟3，等同"过"
        assertEquals(3, g.state.stakes[1])
        assertEquals(2, g.state.turn)
    }

    @Test
    fun 弃牌跳过() {
        val g = game()
        g.call() // 甲跟
        g.fold() // 乙弃
        assertEquals(3, g.state.turn) // 跳过乙直接到丙
    }

    @Test
    fun 两人时一人弃牌即结束() {
        val g = game(players = listOf(Player(1, "甲"), Player(2, "乙")))
        g.call()      // 甲跟1底
        g.call()      // 乙跟1底
        g.fold()      // 甲弃
        assertTrue(g.state.over)
        assertEquals(2, g.state.winnerId)
        assertEquals(mapOf(1 to -1, 2 to 1), g.settlementDeltas())
    }

    @Test
    fun 比牌赢家留下() {
        val g = game()
        g.call() // 甲跟(单张A,9,K)
        g.call() // 乙跟(对K)
        g.call() // 丙跟(顺子)
        g.compare(3) // 甲与丙比，丙顺子赢，甲弃
        assertTrue(1 in g.state.folded)
        assertEquals(2, g.state.turn) // 轮到乙
    }

    @Test
    fun 比牌输者弃() {
        val g = game()
        g.call() // 甲跟
        g.call() // 乙跟
        g.call() // 丙跟
        g.compare(2) // 甲与乙比，乙对K 赢甲单张A
        assertTrue(1 in g.state.folded)
        assertEquals(2, g.state.turn)
    }

    @Test
    fun 比牌平局发起者输() {
        val g = game(
            hands = listOf(
                listOf(p(Rank.KING, Suit.SPADE), p(Rank.KING, Suit.HEART), p(Rank.NINE, Suit.CLUB)),
                listOf(p(Rank.KING, Suit.CLUB), p(Rank.KING, Suit.DIAMOND), p(Rank.NINE, Suit.SPADE)),
                listOf(p(Rank.THREE, Suit.SPADE), p(Rank.FOUR, Suit.HEART), p(Rank.FIVE, Suit.CLUB)),
            ),
        )
        g.call()
        g.call()
        g.call()
        g.compare(2) // 甲与乙对K平局 → 甲输
        assertTrue(1 in g.state.folded)
        assertFalse(2 in g.state.folded)
    }

    @Test
    fun 比牌收比牌费() {
        val g = game()
        g.call()      // 甲跟
        g.call()      // 乙跟
        g.call()      // 丙跟
        g.compare(2)  // 甲(闷)比牌费1底，甲输
        assertEquals(2, g.state.stakes[1])
        assertFalse(g.state.over)
        g.fold()      // 乙弃
        assertTrue(g.state.over)
        assertEquals(3, g.state.winnerId)
    }

    @Test
    fun 结算为零和() {
        val g = game()
        g.raise(3)    // 甲3
        g.call()      // 乙3
        g.fold()      // 丙弃
        g.fold()      // 甲弃
        assertTrue(g.state.over)
        val deltas = g.settlementDeltas()
        assertEquals(0, deltas.values.sum())
        assertEquals(mapOf(1 to -3, 2 to 3, 3 to 0), deltas)
    }

    @Test
    fun 底池为所有投入之和() {
        val g = game(base = 2)
        g.raise(2) // 甲4
        g.call()   // 乙4
        g.fold()   // 丙0
        assertEquals(8, g.pot())
    }
}
