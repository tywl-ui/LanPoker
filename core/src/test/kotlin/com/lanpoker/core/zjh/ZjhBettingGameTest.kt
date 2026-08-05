package com.lanpoker.core.zjh

import com.lanpoker.core.deck.Card
import com.lanpoker.core.deck.Rank
import com.lanpoker.core.deck.Suit
import com.lanpoker.core.ledger.Player
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
        rules: ZjhRules = ZjhRules(),
    ) = ZjhBettingGame(players, hands, base, rules)

    /** 连续跟注直到下注满 3 轮（解锁比牌） */
    private fun ZjhBettingGame.unlockCompare() {
        while (!state.over && state.bettingRounds < 3) {
            call()
        }
    }

    @Test
    fun 下注满3轮才能比牌() {
        val g = game(players = listOf(Player(1, "甲"), Player(2, "乙")))
        assertFalse(g.canCompare())
        assertEquals(null, g.beginCompare(2, null))
        g.call(); g.call() // 第1轮
        assertFalse(g.canCompare())
        g.call(); g.call() // 第2轮
        assertFalse(g.canCompare())
        g.call(); g.call() // 第3轮
        assertTrue(g.canCompare())
        assertEquals(3, g.state.bettingRounds)
        assertNotNull(g.beginCompare(2, null))
    }

    @Test
    fun 下注轮次随行动推进() {
        val g = game()
        g.call() // 甲
        g.call() // 乙
        g.call() // 丙 → 第1轮完成
        assertEquals(1, g.state.bettingRounds)
        g.call() // 甲
        g.call() // 乙
        g.call() // 丙 → 第2轮完成
        assertEquals(2, g.state.bettingRounds)
    }

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
        g.unlockCompare() // 下注满 3 轮后才能比牌
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
        g.unlockCompare() // 下注满 3 轮后才能比牌
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
        g.unlockCompare() // 下注满 3 轮后才能比牌
        g.call()
        g.look() // 乙看牌
        g.call() // 乙看跟
        g.call() // 丙过
        g.compare(2) // 甲与乙对K平局 → 甲输
        assertTrue(1 in g.state.folded)
        assertFalse(2 in g.state.folded)
    }

    @Test
    fun 比牌双方各付比牌费() {
        val g = game()
        g.unlockCompare() // 下注满 3 轮后才能比牌
        g.call()      // 甲过（底注1）
        g.look()      // 乙看牌
        g.call()      // 乙看跟 2底
        g.call()      // 丙过
        g.compare(2)  // 甲(闷)发起比牌：甲付1底，乙(看牌)付2底；甲输
        assertEquals(2, g.state.stakes[1]) // 甲 1+1
        assertEquals(4, g.state.stakes[2]) // 乙 2+2
        assertFalse(g.state.over)
        g.fold()      // 乙弃
        assertTrue(g.state.over)
        assertEquals(3, g.state.winnerId)
    }

    @Test
    fun 三家以上不能与闷牌者比牌() {
        val g = game()
        g.unlockCompare() // 下注满 3 轮后才能比牌
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
        g.unlockCompare() // 下注满 3 轮后才能比牌
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
        g.unlockCompare() // 下注满 3 轮后才能比牌
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
    fun 有王发起者选型定型后赢固定牌() {
        val g = game(
            hands = listOf(
                listOf(Card.Joker(false), p(Rank.ACE, Suit.SPADE), p(Rank.ACE, Suit.HEART)), // 甲 王+对A
                listOf(p(Rank.KING, Suit.SPADE), p(Rank.KING, Suit.HEART), p(Rank.NINE, Suit.CLUB)), // 乙 对K
                listOf(p(Rank.TEN, Suit.SPADE), p(Rank.EIGHT, Suit.HEART), p(Rank.SIX, Suit.CLUB)), // 丙 单张
            ),
        )
        g.unlockCompare() // 下注满 3 轮后才能比牌
        g.call() // 甲过
        g.look() // 乙看牌
        g.call() // 乙看跟
        g.call() // 丙过
        // 甲（有王）锁定豹子A
        val options = g.transformsOf(1)
        val baoZiA = options.first { it.type == ZjhHandType.TRIPLE && it.tie[0] == 14 }
        val r = g.beginCompare(2, baoZiA)
        assertEquals(ZjhBettingGame.CompareStep.RESOLVED, r?.step)
        assertEquals(2, r?.loserId) // 乙对K 输给 豹子A
        assertTrue(2 in g.state.folded)
    }

    @Test
    fun 王防守方选型后发起者输() {
        val g = game(
            hands = listOf(
                listOf(p(Rank.KING, Suit.SPADE), p(Rank.KING, Suit.HEART), p(Rank.NINE, Suit.CLUB)), // 甲 对K
                listOf(Card.Joker(false), p(Rank.ACE, Suit.SPADE), p(Rank.ACE, Suit.HEART)), // 乙 王+对A
                listOf(p(Rank.TEN, Suit.SPADE), p(Rank.EIGHT, Suit.HEART), p(Rank.SIX, Suit.CLUB)),
            ),
        )
        g.unlockCompare() // 下注满 3 轮后才能比牌
        g.call() // 甲过
        g.look() // 乙看牌
        g.call() // 乙看跟
        g.call() // 丙过
        // 甲（无王）发起 vs 乙（有王）
        val r = g.beginCompare(2, null)
        assertEquals(ZjhBettingGame.CompareStep.AWAITING_TARGET, r?.step)
        val wins = r!!.targetOptions
        assertTrue(wins.isNotEmpty())
        assertTrue(wins.all { ZjhEvaluator.compare(it, g.handOf(1)) > 0 }) // 都能赢过甲的对K
        val pick = wins.maxWithOrNull(Comparator { a, b -> ZjhEvaluator.compare(a, b) })!!
        assertTrue(g.finalizeCompare(2, pick))
        assertTrue(1 in g.state.folded) // 甲输
    }

    @Test
    fun 王防守方选不出能赢的牌型则直接输() {
        // 关闭 235 吃豹子：王+23 最多顺子，赢不了豹子A
        val g = game(
            hands = listOf(
                listOf(p(Rank.ACE, Suit.SPADE), p(Rank.ACE, Suit.HEART), p(Rank.ACE, Suit.CLUB)), // 甲 豹子A
                listOf(Card.Joker(false), p(Rank.TWO, Suit.SPADE), p(Rank.THREE, Suit.HEART)), // 乙 王+23
                listOf(p(Rank.TEN, Suit.SPADE), p(Rank.EIGHT, Suit.HEART), p(Rank.SIX, Suit.CLUB)),
            ),
            rules = ZjhRules(rule235EatsTriple = false),
        )
        g.unlockCompare() // 下注满 3 轮后才能比牌
        g.call() // 甲过
        g.look() // 乙看牌
        g.call() // 乙看跟
        g.call() // 丙过
        val r = g.beginCompare(2, null) // 甲豹子A vs 乙
        assertEquals(ZjhBettingGame.CompareStep.RESOLVED, r?.step)
        assertEquals(2, r?.loserId) // 乙最多顺子，赢不了豹子A
        assertTrue(2 in g.state.folded)
    }

    @Test
    fun 王防守方可变235吃豹子() {
        // 235 吃豹子开启时：王+23 能变 235 吃掉豹子A
        val g = game(
            hands = listOf(
                listOf(p(Rank.ACE, Suit.SPADE), p(Rank.ACE, Suit.HEART), p(Rank.ACE, Suit.CLUB)), // 甲 豹子A
                listOf(Card.Joker(false), p(Rank.TWO, Suit.SPADE), p(Rank.THREE, Suit.HEART)), // 乙 王+23
                listOf(p(Rank.TEN, Suit.SPADE), p(Rank.EIGHT, Suit.HEART), p(Rank.SIX, Suit.CLUB)),
            ),
        )
        g.unlockCompare() // 下注满 3 轮后才能比牌
        g.call() // 甲过
        g.look() // 乙看牌
        g.call() // 乙看跟
        g.call() // 丙过
        val r = g.beginCompare(2, null)
        assertEquals(ZjhBettingGame.CompareStep.AWAITING_TARGET, r?.step)
        assertTrue(r!!.targetOptions.any { it.type == ZjhHandType.SPECIAL_235 })
        val pick235 = r.targetOptions.first { it.type == ZjhHandType.SPECIAL_235 }
        assertTrue(g.finalizeCompare(2, pick235))
        assertTrue(1 in g.state.folded) // 甲豹子A 被 235 吃掉
    }

    @Test
    fun 双方有王发起者定型后防守方无解则输() {
        val g = game(
            hands = listOf(
                listOf(Card.Joker(false), p(Rank.ACE, Suit.SPADE), p(Rank.ACE, Suit.HEART)), // 甲 王+对A
                listOf(Card.Joker(false), p(Rank.KING, Suit.SPADE), p(Rank.KING, Suit.HEART)), // 乙 王+对K
                listOf(p(Rank.TEN, Suit.SPADE), p(Rank.EIGHT, Suit.HEART), p(Rank.SIX, Suit.CLUB)),
            ),
        )
        g.unlockCompare() // 下注满 3 轮后才能比牌
        g.call() // 甲过
        g.look() // 乙看牌
        g.call() // 乙看跟
        g.call() // 丙过
        val baoZiA = g.transformsOf(1).first { it.type == ZjhHandType.TRIPLE && it.tie[0] == 14 }
        val r = g.beginCompare(2, baoZiA)
        assertEquals(ZjhBettingGame.CompareStep.RESOLVED, r?.step)
        assertEquals(2, r?.loserId) // 乙最大豹子K < 豹子A，无解
        assertTrue(2 in g.state.folded)
    }

    @Test
    fun 非法选型被拒绝() {
        val g = game(
            hands = listOf(
                listOf(Card.Joker(false), p(Rank.TWO, Suit.SPADE), p(Rank.THREE, Suit.HEART)), // 甲 王+23
                listOf(p(Rank.KING, Suit.SPADE), p(Rank.KING, Suit.HEART), p(Rank.NINE, Suit.CLUB)),
                listOf(p(Rank.TEN, Suit.SPADE), p(Rank.EIGHT, Suit.HEART), p(Rank.SIX, Suit.CLUB)),
            ),
        )
        g.unlockCompare() // 下注满 3 轮后才能比牌
        g.call()
        g.look() // 乙看牌
        g.call()
        g.call()
        // 甲手里王+23 不可能变出豹子A → 拒绝
        val fake = ZjhEvaluator.evaluate(
            listOf(p(Rank.ACE, Suit.SPADE), p(Rank.ACE, Suit.HEART), p(Rank.ACE, Suit.CLUB)),
        )
        assertEquals(null, g.beginCompare(2, fake))
        assertEquals(0, g.state.folded.size)
    }

    @Test
    fun 非王玩家变换列表只有最优一项() {
        val g = game()
        assertEquals(1, g.transformsOf(1).size)
        assertFalse(g.hasJoker(1))
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
