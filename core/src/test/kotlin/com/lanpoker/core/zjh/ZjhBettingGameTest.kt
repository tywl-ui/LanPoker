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
    fun 开局每人自动下底注() {
        val g = game(base = 2)
        assertEquals(mapOf(1 to 2, 2 to 2, 3 to 2), g.state.stakes)
    }

    @Test
    fun 闷牌跟注与底注齐平视为过() {
        val g = game(base = 2)
        g.call() // 闷跟 req=2，已投2 → 过
        assertEquals(2, g.state.stakes[1])
        assertEquals(2, g.state.turn)
        assertTrue(g.state.lastAction!!.contains("过"))
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
    fun 加注不能超过上限() {
        val g = game()
        assertTrue(g.raise(10))
        assertFalse(g.raise(11))
        assertEquals(10, g.state.level)
    }

    @Test
    fun 看牌后加注按新level的2倍补齐() {
        val g = game()
        g.look()      // 甲看牌
        g.raise(3)    // 甲看牌加到3 → 6底
        assertEquals(6, g.state.stakes[1])
        g.call()      // 乙闷跟3
        g.call()      // 丙闷跟3
        g.raise(4)    // 甲再次加到4 → 看牌 8底（更高）
        assertEquals(8, g.state.stakes[1])
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
        g.call() // 甲过
        g.fold() // 乙弃
        assertEquals(3, g.state.turn) // 跳过乙直接到丙
    }

    @Test
    fun 两人时一人弃牌即结束() {
        val g = game(players = listOf(Player(1, "甲"), Player(2, "乙")))
        g.call()      // 甲过
        g.fold()      // 乙弃（底注留下）
        assertTrue(g.state.over)
        assertEquals(1, g.state.winnerId)
        assertEquals(mapOf(1 to 1, 2 to -1), g.settlementDeltas())
    }

    @Test
    fun 比牌赢家留下() {
        val g = game()
        g.call() // 甲过
        g.look() // 乙看牌
        g.call() // 乙看跟 2底
        g.look() // 丙看牌
        g.call() // 丙看跟 2底
        g.compare(3) // 甲与丙比（丙已看牌，合法），丙顺子赢，甲弃
        assertTrue(1 in g.state.folded)
        assertEquals(2, g.state.turn) // 轮到乙
    }

    @Test
    fun 比牌输者弃() {
        val g = game()
        g.call()
        g.look() // 乙看牌
        g.call() // 乙看跟
        g.call() // 丙过
        g.compare(2) // 甲与乙比（乙已看牌，合法），乙对K 赢甲单张A
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
        g.look() // 乙看牌
        g.call() // 乙看跟
        g.call() // 丙过
        g.compare(2) // 甲与乙对K平局 → 甲输
        assertTrue(1 in g.state.folded)
        assertFalse(2 in g.state.folded)
    }

    @Test
    fun 比牌收比牌费() {
        val g = game()
        g.call()      // 甲过
        g.look()      // 乙看牌
        g.call()      // 乙看跟 2底
        g.call()      // 丙过
        g.compare(2)  // 甲(闷)比牌费1底，甲输
        assertEquals(2, g.state.stakes[1])
        assertFalse(g.state.over)
        g.fold()      // 乙弃
        assertTrue(g.state.over)
        assertEquals(3, g.state.winnerId)
    }

    @Test
    fun 三家以上不能与闷牌者比牌() {
        val g = game()
        g.call() // 甲过
        g.call() // 乙过
        g.call() // 丙过
        // 三家都在，乙、丙都是闷牌 → 比牌被拒
        assertFalse(g.compare(2))
        assertFalse(g.compare(3))
        assertEquals(0, g.state.folded.size)
    }

    @Test
    fun 三家以上可与看牌者比牌() {
        val g = game()
        g.call()      // 甲过
        g.look()      // 乙看牌
        g.call()      // 乙看跟 2底
        g.call()      // 丙过
        // 轮到甲，三家在场，乙已看牌 → 允许
        assertTrue(g.compare(2))
        assertTrue(1 in g.state.folded) // 甲单张A 输给 乙对K
    }

    @Test
    fun 仅剩两家可与闷牌者开牌() {
        val g = game()
        g.call() // 甲过
        g.call() // 乙过
        g.call() // 丙过
        g.fold() // 甲弃 → 剩乙、丙
        // 乙与闷牌的丙比牌（两家时可以开闷牌）
        assertTrue(g.compare(3))
        assertTrue(2 in g.state.folded) // 乙对K 输给 丙顺子
        assertTrue(g.state.over)
        assertEquals(3, g.state.winnerId)
    }

    @Test
    fun 结算为零和() {
        val g = game()
        g.raise(3)    // 甲3
        g.call()      // 乙3
        g.fold()      // 丙弃（底注1留下）
        g.fold()      // 甲弃
        assertTrue(g.state.over)
        val deltas = g.settlementDeltas()
        assertEquals(0, deltas.values.sum())
        assertEquals(mapOf(1 to -3, 2 to 4, 3 to -1), deltas)
    }

    @Test
    fun 底池为所有投入之和() {
        val g = game(base = 2)
        g.raise(2) // 甲4
        g.call()   // 乙4
        g.fold()   // 丙底注2留下
        assertEquals(10, g.pot())
    }
}
