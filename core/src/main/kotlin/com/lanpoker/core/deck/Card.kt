package com.lanpoker.core.deck

/** 花色，SPADE 最大，DIAMOND 最小（用于平局比花色） */
enum class Suit(val symbol: String, val suitOrder: Int) {
    SPADE("♠", 4),
    HEART("♥", 3),
    CLUB("♣", 2),
    DIAMOND("♦", 1),
}

/** 点数，2 最小，A 最大（value 为自然大小，游戏内特殊排序由各玩法引擎处理） */
enum class Rank(val label: String, val value: Int) {
    TWO("2", 2),
    THREE("3", 3),
    FOUR("4", 4),
    FIVE("5", 5),
    SIX("6", 6),
    SEVEN("7", 7),
    EIGHT("8", 8),
    NINE("9", 9),
    TEN("10", 10),
    JACK("J", 11),
    QUEEN("Q", 12),
    KING("K", 13),
    ACE("A", 14),
}

/** 一张牌 */
sealed class Card {
    abstract val label: String

    data class Poker(val rank: Rank, val suit: Suit) : Card() {
        override val label: String get() = rank.label + suit.symbol
    }

    data class Joker(val big: Boolean) : Card() {
        override val label: String get() = if (big) "大王" else "小王"
    }
}
