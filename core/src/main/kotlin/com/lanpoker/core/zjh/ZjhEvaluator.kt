package com.lanpoker.core.zjh

import com.lanpoker.core.deck.Card
import com.lanpoker.core.deck.Rank
import com.lanpoker.core.deck.Suit

/** 炸金花牌型，从小到大。SPECIAL_235 单独处理，见 [ZjhEvaluator.compare] */
enum class ZjhHandType(val order: Int, val label: String) {
    HIGH_CARD(1, "单张"),
    PAIR(2, "对子"),
    STRAIGHT(3, "顺子"),
    FLUSH(4, "金花"),
    STRAIGHT_FLUSH(5, "顺金"),
    TRIPLE(6, "豹子"),
    SPECIAL_235(1, "235"),
}

/** 规则开关 */
data class ZjhRules(
    /** 杂色 235 吃豹子（同花 235 算金花，不触发） */
    val rule235EatsTriple: Boolean = true,
    /** 允许同花豹（三张同花色同点数）：仅 3 副牌时可能真实存在；1-2 副牌时王不能变成同花豹 */
    val allowSameSuitTriple: Boolean = false,
)

/** 一副已判定的手牌：tie 为按序比较的大小键（从大到小） */
data class ZjhHand(
    val type: ZjhHandType,
    val tie: List<Int>,
    val cards: List<Card>,
)

/** 牌型点数完全相同时的处理方式 */
enum class TieRule(val label: String) {
    REDEAL("和局重发"),
    SUIT("比花色"),
    MONEY_BACK("平局退钱"),
}

object ZjhEvaluator {

    /**
     * 判定 3 张牌。
     * 王为百搭：暴力枚举每个王可能代替的牌，取能组成最大牌型的组合。
     * 注意：返回的 cards 永远是【真实手牌】（含王），只有 tie/type 来自最优组合，
     * 避免亮牌/描述时把王显示成不存在的牌。
     */
    fun evaluate(hand: List<Card>, rules: ZjhRules = ZjhRules()): ZjhHand {
        require(hand.size == 3) { "炸金花每人 3 张牌" }
        val jokers = hand.filterIsInstance<Card.Joker>()
        val normals = hand.filterIsInstance<Card.Poker>()
        if (jokers.isEmpty()) return evaluateNormal(normals, rules)

        val variants = Rank.entries.flatMap { r -> Suit.entries.map { s -> Card.Poker(r, s) } }
        var best: ZjhHand? = null
        fun rec(idx: Int, cur: List<Card>) {
            if (idx == jokers.size) {
                val h = evaluateNormal(cur.filterIsInstance<Card.Poker>(), rules)
                // 1-2 副牌不允许同花豹：王不能变成三张同花色同点数
                if (!rules.allowSameSuitTriple && isSameSuitTriple(h)) return
                if (best == null || compare(h, best!!) > 0) best = h
                return
            }
            for (v in variants) rec(idx + 1, cur + v)
        }
        rec(0, normals)
        // 用真实手牌替换枚举出的虚拟牌（type/tie 保留最优结果）
        return best!!.copy(cards = hand)
    }

    /** 是否同花豹（三张同花色同点数） */
    internal fun isSameSuitTriple(h: ZjhHand): Boolean =
        h.type == ZjhHandType.TRIPLE && h.cards.size == 3 &&
            h.cards.all { it is Card.Poker && it.suit == (h.cards[0] as Card.Poker).suit }

    /** 无王的判定 */
    internal fun evaluateNormal(cards: List<Card.Poker>, rules: ZjhRules): ZjhHand {
        require(cards.size == 3)
        val sorted = cards.sortedByDescending { it.rank.value }
        val values = sorted.map { it.rank.value }
        val isTriple = values[0] == values[1] && values[1] == values[2]
        val isFlush = sorted.all { it.suit == sorted[0].suit }
        val straightHigh = straightHigh(values)
        return when {
            isTriple -> ZjhHand(ZjhHandType.TRIPLE, listOf(values[0]), sorted)
            straightHigh != null && isFlush -> ZjhHand(ZjhHandType.STRAIGHT_FLUSH, listOf(straightHigh), sorted)
            isFlush -> ZjhHand(ZjhHandType.FLUSH, values, sorted)
            straightHigh != null -> ZjhHand(ZjhHandType.STRAIGHT, listOf(straightHigh), sorted)
            values[0] == values[1] -> ZjhHand(ZjhHandType.PAIR, listOf(values[0], values[2]), sorted)
            values[1] == values[2] -> ZjhHand(ZjhHandType.PAIR, listOf(values[1], values[0]), sorted)
            rules.rule235EatsTriple && values.toSet() == setOf(2, 3, 5) ->
                ZjhHand(ZjhHandType.SPECIAL_235, values, sorted)
            else -> ZjhHand(ZjhHandType.HIGH_CARD, values, sorted)
        }
    }

    /** 顺子的最大张，非顺子返回 null。A 可作低（A23）可作高（QKA） */
    internal fun straightHigh(values: List<Int>): Int? {
        val s = values.sorted()
        return when {
            s[2] - s[1] == 1 && s[1] - s[0] == 1 -> s[2]
            s == listOf(2, 3, 14) -> 3
            else -> null
        }
    }

    /**
     * 比较两手牌。>0 表示 a 赢，<0 表示 b 赢，0 表示牌型点数平局。
     * 235 规则：SPECIAL_235（杂色）只压 TRIPLE，其余情况视为单张；
     * 金花 235 吃同花豹；同点数时同花豹 > 杂花豹（3 副牌玩法）。
     */
    fun compare(a: ZjhHand, b: ZjhHand): Int {
        val a235 = a.type == ZjhHandType.SPECIAL_235
        val b235 = b.type == ZjhHandType.SPECIAL_235
        if (a235 && b235) return compareTie(a, b)
        if (a235) return if (b.type == ZjhHandType.TRIPLE) 1 else -1
        if (b235) return if (a.type == ZjhHandType.TRIPLE) -1 else 1
        val aFlush235 = a.type == ZjhHandType.FLUSH && a.tie == listOf(5, 3, 2)
        val bFlush235 = b.type == ZjhHandType.FLUSH && b.tie == listOf(5, 3, 2)
        val aSuitTriple = isSameSuitTriple(a)
        val bSuitTriple = isSameSuitTriple(b)
        // 金花 235 吃同花豹（对杂花豹无效，按正常顺序）
        if (aFlush235 && bSuitTriple) return 1
        if (bFlush235 && aSuitTriple) return -1
        if (a.type != b.type) return a.type.order - b.type.order
        if (a.type == ZjhHandType.TRIPLE) {
            val cmp = compareTie(a, b)
            if (cmp != 0) return cmp
            // 点数相同：同花豹 > 杂花豹
            if (aSuitTriple != bSuitTriple) return if (aSuitTriple) 1 else -1
            return 0
        }
        return compareTie(a, b)
    }

    private fun compareTie(a: ZjhHand, b: ZjhHand): Int {
        for (i in 0 until minOf(a.tie.size, b.tie.size)) {
            if (a.tie[i] != b.tie[i]) return a.tie[i] - b.tie[i]
        }
        return 0
    }

    /** 多副牌比花色：按（点数,花色）键从大到小逐张比，先分出胜负者赢；全平返回 0 */
    private fun compareBySuit(a: ZjhHand, b: ZjhHand): Int {
        fun key(c: Card): Int = when (c) {
            is Card.Poker -> c.rank.value * 4 + c.suit.suitOrder
            is Card.Joker -> 0
        }
        val ka = a.cards.map(::key).sortedDescending()
        val kb = b.cards.map(::key).sortedDescending()
        for (i in 0 until minOf(ka.size, kb.size)) {
            if (ka[i] != kb[i]) return ka[i] - kb[i]
        }
        return 0
    }

    /**
     * 在 N 手牌中选出赢家。
     * @return 赢家下标；null 表示平局（按 tieRule 无法分出胜负 → 和局重发）
     */
    fun strongestIndex(hands: List<ZjhHand>, tieRule: TieRule): Int? {
        require(hands.isNotEmpty())
        var winner = 0
        for (i in 1 until hands.size) {
            val c = compare(hands[winner], hands[i])
            when {
                c < 0 -> winner = i
                c > 0 -> Unit
                else -> {
                    if (tieRule == TieRule.SUIT) {
                        val sc = compareBySuit(hands[winner], hands[i])
                        if (sc < 0) winner = i
                        else if (sc > 0) Unit
                        else return null
                    } else {
                        return null
                    }
                }
            }
        }
        return winner
    }

    /** 牌型中文描述，如 "豹子A"、"顺金QKA"、"235" */
    fun describe(hand: ZjhHand): String {
        val c = hand.cards.map { it.label }.joinToString("")
        return "${hand.type.label}$c"
    }

    /** 点数 → 牌面 */
    fun rankLabel(v: Int): String = when (v) {
        14 -> "A"; 13 -> "K"; 12 -> "Q"; 11 -> "J"; else -> "$v"
    }

    /** 牌型选项的简短标签（王选型对话框用），如 "豹子A"、"顺子A高"、"对K带9" */
    fun optionLabel(hand: ZjhHand): String = when (hand.type) {
        ZjhHandType.TRIPLE -> "豹子${rankLabel(hand.tie[0])}"
        ZjhHandType.STRAIGHT_FLUSH -> "顺金${rankLabel(hand.tie[0])}高"
        ZjhHandType.FLUSH -> "金花${hand.tie.joinToString("") { rankLabel(it) }}"
        ZjhHandType.STRAIGHT -> "顺子${rankLabel(hand.tie[0])}高"
        ZjhHandType.PAIR -> "对${rankLabel(hand.tie[0])}带${rankLabel(hand.tie[1])}"
        ZjhHandType.HIGH_CARD -> "单张${hand.tie.joinToString("") { rankLabel(it) }}"
        ZjhHandType.SPECIAL_235 -> "235（吃豹子）"
    }
}
