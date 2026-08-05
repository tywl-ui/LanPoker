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

class ZjhEvaluatorTest {

    private fun p(rank: Rank, suit: Suit) = Card.Poker(rank, suit)

    private fun type(vararg cards: Card): ZjhHandType = ZjhEvaluator.evaluate(cards.toList()).type

    // ---------- 基础牌型 ----------

    @Test
    fun 单张() {
        assertEquals(ZjhHandType.HIGH_CARD, type(p(Rank.ACE, Suit.SPADE), p(Rank.KING, Suit.HEART), p(Rank.NINE, Suit.CLUB)))
    }

    @Test
    fun 对子() {
        assertEquals(ZjhHandType.PAIR, type(p(Rank.ACE, Suit.SPADE), p(Rank.ACE, Suit.HEART), p(Rank.NINE, Suit.CLUB)))
    }

    @Test
    fun 顺子() {
        assertEquals(ZjhHandType.STRAIGHT, type(p(Rank.THREE, Suit.SPADE), p(Rank.FOUR, Suit.HEART), p(Rank.FIVE, Suit.CLUB)))
    }

    @Test
    fun 金花() {
        assertEquals(ZjhHandType.FLUSH, type(p(Rank.ACE, Suit.SPADE), p(Rank.KING, Suit.SPADE), p(Rank.NINE, Suit.SPADE)))
    }

    @Test
    fun 顺金() {
        assertEquals(ZjhHandType.STRAIGHT_FLUSH, type(p(Rank.QUEEN, Suit.HEART), p(Rank.KING, Suit.HEART), p(Rank.ACE, Suit.HEART)))
    }

    @Test
    fun 豹子() {
        assertEquals(ZjhHandType.TRIPLE, type(p(Rank.ACE, Suit.SPADE), p(Rank.ACE, Suit.HEART), p(Rank.ACE, Suit.CLUB)))
    }

    // ---------- A 作低作高 ----------

    @Test
    fun A23是顺子() {
        assertEquals(ZjhHandType.STRAIGHT, type(p(Rank.ACE, Suit.SPADE), p(Rank.TWO, Suit.HEART), p(Rank.THREE, Suit.CLUB)))
    }

    @Test
    fun KA2不是顺子() {
        assertTrue(ZjhEvaluator.straightHigh(listOf(13, 14, 2)) == null)
    }

    @Test
    fun QKA是顺金() {
        assertEquals(ZjhHandType.STRAIGHT_FLUSH, type(p(Rank.QUEEN, Suit.CLUB), p(Rank.KING, Suit.CLUB), p(Rank.ACE, Suit.CLUB)))
    }

    // ---------- 235 规则 ----------

    @Test
    fun 杂色235吃豹子() {
        val a = ZjhEvaluator.evaluate(listOf(p(Rank.TWO, Suit.SPADE), p(Rank.THREE, Suit.HEART), p(Rank.FIVE, Suit.CLUB)))
        val b = ZjhEvaluator.evaluate(listOf(p(Rank.ACE, Suit.SPADE), p(Rank.ACE, Suit.HEART), p(Rank.ACE, Suit.CLUB)))
        assertTrue(ZjhEvaluator.compare(a, b) > 0)
        assertEquals(ZjhHandType.SPECIAL_235, a.type)
    }

    @Test
    fun 杂色235打不过对子() {
        val a = ZjhEvaluator.evaluate(listOf(p(Rank.TWO, Suit.SPADE), p(Rank.THREE, Suit.HEART), p(Rank.FIVE, Suit.CLUB)))
        val b = ZjhEvaluator.evaluate(listOf(p(Rank.KING, Suit.SPADE), p(Rank.KING, Suit.HEART), p(Rank.NINE, Suit.CLUB)))
        assertTrue(ZjhEvaluator.compare(a, b) < 0)
    }

    @Test
    fun 同花235不算235() {
        val h = ZjhEvaluator.evaluate(listOf(p(Rank.TWO, Suit.HEART), p(Rank.THREE, Suit.HEART), p(Rank.FIVE, Suit.HEART)))
        assertEquals(ZjhHandType.FLUSH, h.type)
    }

    @Test
    fun 关闭235规则() {
        val rules = ZjhRules(rule235EatsTriple = false)
        val h = ZjhEvaluator.evaluate(listOf(p(Rank.TWO, Suit.SPADE), p(Rank.THREE, Suit.HEART), p(Rank.FIVE, Suit.CLUB)), rules)
        assertEquals(ZjhHandType.HIGH_CARD, h.type)
    }

    // ---------- 王当百搭 ----------

    @Test
    fun 王加对A是豹子A() {
        val h = ZjhEvaluator.evaluate(listOf(Card.Joker(false), p(Rank.ACE, Suit.SPADE), p(Rank.ACE, Suit.HEART)))
        assertEquals(ZjhHandType.TRIPLE, h.type)
        assertEquals(14, h.tie[0])
    }

    @Test
    fun 王加AK同花是顺金() {
        val h = ZjhEvaluator.evaluate(listOf(Card.Joker(false), p(Rank.ACE, Suit.HEART), p(Rank.KING, Suit.HEART)))
        assertEquals(ZjhHandType.STRAIGHT_FLUSH, h.type)
    }

    @Test
    fun 王百搭亮牌显示真实手牌() {
        val hand = listOf(Card.Joker(false), p(Rank.ACE, Suit.SPADE), p(Rank.ACE, Suit.HEART))
        val h = ZjhEvaluator.evaluate(hand)
        // 牌组必须还是真实手牌（含王），不能显示枚举出的假牌
        assertEquals(hand, h.cards)
        assertEquals("豹子小王A♠A♥", ZjhEvaluator.describe(h))
    }

    @Test
    fun 王加23优先顺子而不是235() {
        val h = ZjhEvaluator.evaluate(listOf(Card.Joker(false), p(Rank.TWO, Suit.SPADE), p(Rank.THREE, Suit.HEART)))
        // 王=4 → 234 顺子，比 235 大（235 只吃豹子）
        assertEquals(ZjhHandType.STRAIGHT, h.type)
    }

    @Test
    fun 双王单A取最大牌型() {
        val h = ZjhEvaluator.evaluate(listOf(Card.Joker(false), Card.Joker(true), p(Rank.ACE, Suit.SPADE)))
        assertEquals(ZjhHandType.TRIPLE, h.type)
    }

    @Test
    fun 三王是豹子() {
        val h = ZjhEvaluator.evaluate(listOf(Card.Joker(false), Card.Joker(false), Card.Joker(true)))
        assertEquals(ZjhHandType.TRIPLE, h.type)
    }

    // ---------- 大小比较 ----------

    @Test
    fun 豹子大于顺金() {
        val a = ZjhEvaluator.evaluate(listOf(p(Rank.ACE, Suit.SPADE), p(Rank.ACE, Suit.HEART), p(Rank.ACE, Suit.CLUB)))
        val b = ZjhEvaluator.evaluate(listOf(p(Rank.QUEEN, Suit.HEART), p(Rank.KING, Suit.HEART), p(Rank.ACE, Suit.HEART)))
        assertTrue(ZjhEvaluator.compare(a, b) > 0)
    }

    @Test
    fun 同型比点数() {
        val a = ZjhEvaluator.evaluate(listOf(p(Rank.ACE, Suit.SPADE), p(Rank.KING, Suit.HEART), p(Rank.NINE, Suit.CLUB)))
        val b = ZjhEvaluator.evaluate(listOf(p(Rank.KING, Suit.SPADE), p(Rank.QUEEN, Suit.HEART), p(Rank.TEN, Suit.CLUB)))
        assertTrue(ZjhEvaluator.compare(a, b) > 0)
    }

    @Test
    fun 顺子A23最小QKA最大() {
        val low = ZjhEvaluator.evaluate(listOf(p(Rank.ACE, Suit.SPADE), p(Rank.TWO, Suit.HEART), p(Rank.THREE, Suit.CLUB)))
        val high = ZjhEvaluator.evaluate(listOf(p(Rank.QUEEN, Suit.SPADE), p(Rank.KING, Suit.HEART), p(Rank.ACE, Suit.CLUB)))
        assertTrue(ZjhEvaluator.compare(high, low) > 0)
    }

    // ---------- 平局规则 ----------

    @Test
    fun 两副牌同点不同花按重发为平局() {
        val a = ZjhEvaluator.evaluate(listOf(p(Rank.KING, Suit.SPADE), p(Rank.KING, Suit.HEART), p(Rank.NINE, Suit.CLUB)))
        val b = ZjhEvaluator.evaluate(listOf(p(Rank.KING, Suit.CLUB), p(Rank.KING, Suit.DIAMOND), p(Rank.NINE, Suit.SPADE)))
        assertEquals(0, ZjhEvaluator.compare(a, b))
        assertNull(ZjhEvaluator.strongestIndex(listOf(a, b), TieRule.REDEAL))
    }

    @Test
    fun 完全相同的牌重发也为平局() {
        val a = ZjhEvaluator.evaluate(listOf(p(Rank.KING, Suit.SPADE), p(Rank.KING, Suit.HEART), p(Rank.NINE, Suit.CLUB)))
        val b = ZjhEvaluator.evaluate(listOf(p(Rank.KING, Suit.SPADE), p(Rank.KING, Suit.HEART), p(Rank.NINE, Suit.CLUB)))
        assertNull(ZjhEvaluator.strongestIndex(listOf(a, b), TieRule.REDEAL))
    }

    @Test
    fun 比花色分出胜负() {
        val a = ZjhEvaluator.evaluate(listOf(p(Rank.KING, Suit.SPADE), p(Rank.KING, Suit.HEART), p(Rank.NINE, Suit.CLUB)))
        val b = ZjhEvaluator.evaluate(listOf(p(Rank.KING, Suit.CLUB), p(Rank.KING, Suit.DIAMOND), p(Rank.NINE, Suit.SPADE)))
        assertEquals(0, ZjhEvaluator.strongestIndex(listOf(a, b), TieRule.SUIT))
    }

    @Test
    fun 平局退钱返回null() {
        val a = ZjhEvaluator.evaluate(listOf(p(Rank.KING, Suit.SPADE), p(Rank.KING, Suit.HEART), p(Rank.NINE, Suit.CLUB)))
        val b = ZjhEvaluator.evaluate(listOf(p(Rank.KING, Suit.CLUB), p(Rank.KING, Suit.DIAMOND), p(Rank.NINE, Suit.SPADE)))
        assertNull(ZjhEvaluator.strongestIndex(listOf(a, b), TieRule.MONEY_BACK))
    }

    @Test
    fun 多人中选出最大() {
        val hands = listOf(
            ZjhEvaluator.evaluate(listOf(p(Rank.ACE, Suit.SPADE), p(Rank.KING, Suit.HEART), p(Rank.NINE, Suit.CLUB))),
            ZjhEvaluator.evaluate(listOf(p(Rank.KING, Suit.CLUB), p(Rank.KING, Suit.DIAMOND), p(Rank.NINE, Suit.SPADE))),
            ZjhEvaluator.evaluate(listOf(p(Rank.TWO, Suit.SPADE), p(Rank.TWO, Suit.HEART), p(Rank.THREE, Suit.CLUB))),
        )
        assertEquals(1, ZjhEvaluator.strongestIndex(hands, TieRule.REDEAL))
    }

    @Test
    fun 多人中任意两人平局则整体重发() {
        // 甲、乙同点数对K，丙单张 → 全局和局
        val hands = listOf(
            ZjhEvaluator.evaluate(listOf(p(Rank.KING, Suit.SPADE), p(Rank.KING, Suit.HEART), p(Rank.NINE, Suit.CLUB))),
            ZjhEvaluator.evaluate(listOf(p(Rank.KING, Suit.CLUB), p(Rank.KING, Suit.DIAMOND), p(Rank.NINE, Suit.SPADE))),
            ZjhEvaluator.evaluate(listOf(p(Rank.ACE, Suit.SPADE), p(Rank.QUEEN, Suit.HEART), p(Rank.TEN, Suit.CLUB))),
        )
        assertEquals(null, ZjhEvaluator.strongestIndex(hands, TieRule.REDEAL))
    }

    @Test
    fun 金花235吃同花豹() {
        val flush235 = ZjhEvaluator.evaluate(listOf(p(Rank.TWO, Suit.HEART), p(Rank.THREE, Suit.HEART), p(Rank.FIVE, Suit.HEART)))
        val sameSuitTriple = ZjhEvaluator.evaluate(listOf(p(Rank.ACE, Suit.SPADE), p(Rank.ACE, Suit.SPADE), p(Rank.ACE, Suit.SPADE)))
        assertEquals(ZjhHandType.FLUSH, flush235.type)
        assertTrue(ZjhEvaluator.isSameSuitTriple(sameSuitTriple))
        assertTrue(ZjhEvaluator.compare(flush235, sameSuitTriple) > 0)  // 金花235 吃 同花豹
    }

    @Test
    fun 金花235输给杂花豹() {
        val flush235 = ZjhEvaluator.evaluate(listOf(p(Rank.TWO, Suit.HEART), p(Rank.THREE, Suit.HEART), p(Rank.FIVE, Suit.HEART)))
        val mixedTriple = ZjhEvaluator.evaluate(listOf(p(Rank.ACE, Suit.SPADE), p(Rank.ACE, Suit.HEART), p(Rank.ACE, Suit.CLUB)))
        assertFalse(ZjhEvaluator.isSameSuitTriple(mixedTriple))
        assertTrue(ZjhEvaluator.compare(flush235, mixedTriple) < 0)  // 杂花豹 赢 金花235
    }

    @Test
    fun 同花豹大于杂花豹() {
        val same = ZjhEvaluator.evaluate(listOf(p(Rank.ACE, Suit.SPADE), p(Rank.ACE, Suit.SPADE), p(Rank.ACE, Suit.SPADE)))
        val mixed = ZjhEvaluator.evaluate(listOf(p(Rank.ACE, Suit.SPADE), p(Rank.ACE, Suit.HEART), p(Rank.ACE, Suit.CLUB)))
        assertTrue(ZjhEvaluator.compare(same, mixed) > 0)
        assertEquals(0, ZjhEvaluator.compare(same, same)) // 两张相同的同花豹平局
    }

    @Test
    fun 一副牌时王不能变同花豹() {
        val g = ZjhBettingGame(
            players = listOf(Player(1, "甲"), Player(2, "乙")),
            hands = listOf(
                listOf(Card.Joker(false), p(Rank.ACE, Suit.SPADE), p(Rank.ACE, Suit.SPADE)),
                listOf(p(Rank.TEN, Suit.SPADE), p(Rank.EIGHT, Suit.HEART), p(Rank.SIX, Suit.CLUB)),
            ),
            base = 1,
        )
        val triples = g.transformsOf(1).filter { it.type == ZjhHandType.TRIPLE }
        assertTrue(triples.isNotEmpty())
        assertTrue(triples.all { !ZjhEvaluator.isSameSuitTriple(it) })
    }

    @Test
    fun 三副牌时王可以变同花豹() {
        val g = ZjhBettingGame(
            players = listOf(Player(1, "甲"), Player(2, "乙")),
            hands = listOf(
                listOf(Card.Joker(false), p(Rank.ACE, Suit.SPADE), p(Rank.ACE, Suit.SPADE)),
                listOf(p(Rank.TEN, Suit.SPADE), p(Rank.EIGHT, Suit.HEART), p(Rank.SIX, Suit.CLUB)),
            ),
            base = 1,
            rules = ZjhRules(allowSameSuitTriple = true),
        )
        assertTrue(g.transformsOf(1).any { ZjhEvaluator.isSameSuitTriple(it) })
    }

    @Test
    fun 描述() {
        val h = ZjhEvaluator.evaluate(listOf(p(Rank.ACE, Suit.SPADE), p(Rank.ACE, Suit.HEART), p(Rank.ACE, Suit.CLUB)))
        assertEquals("豹子A♠A♥A♣", ZjhEvaluator.describe(h))
    }
}
