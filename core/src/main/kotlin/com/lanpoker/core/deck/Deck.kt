package com.lanpoker.core.deck

import kotlin.random.Random

/**
 * 牌堆：支持 1-3 副牌 + 0-6 张王（最多两副的王数）。
 * 王的分配规则：前一半大、后一半小（奇数张时多一张大? 不，均分，奇数时小王多一张）。
 */
class Deck private constructor(
    val cards: List<Card>,
    val deckCount: Int,
    val jokerCount: Int,
) {
    companion object {
        const val MAX_DECK = 3
        const val MAX_JOKERS = 6

        fun build(deckCount: Int, jokerCount: Int): Deck {
            require(deckCount in 1..MAX_DECK) { "deckCount 需在 1..$MAX_DECK" }
            require(jokerCount in 0..MAX_JOKERS) { "jokerCount 需在 0..$MAX_JOKERS" }
            require(jokerCount <= deckCount * 2) { "王数量不能超过 ${deckCount * 2}（$deckCount 副）" }

            val cards = mutableListOf<Card>()
            repeat(deckCount) {
                for (r in Rank.entries) {
                    for (s in Suit.entries) {
                        cards += Card.Poker(r, s)
                    }
                }
            }
            val big = jokerCount / 2
            val small = jokerCount - big
            repeat(big) { cards += Card.Joker(big = true) }
            repeat(small) { cards += Card.Joker(big = false) }
            return Deck(cards, deckCount, jokerCount)
        }
    }

    fun shuffled(random: Random = Random.Default): List<Card> = cards.shuffled(random)

    /**
     * 洗牌并发牌。
     * @param handSize 每人手牌数
     * @param playerCount 人数
     * @param extra 额外保留的牌（斗地主底牌等），从牌堆顶取出
     * @return Pair(各家手牌, 额外牌)
     */
    fun deal(handSize: Int, playerCount: Int, extra: Int = 0, random: Random = Random.Default): Pair<List<List<Card>>, List<Card>> {
        require(handSize * playerCount + extra <= cards.size) { "牌数不足" }
        val shuffled = shuffled(random)
        val extraCards = shuffled.take(extra)
        val pool = shuffled.drop(extra)
        val hands = (0 until playerCount).map { p ->
            pool.drop(p * handSize).take(handSize)
        }
        return hands to extraCards
    }
}
