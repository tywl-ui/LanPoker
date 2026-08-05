package com.lanpoker.app.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.lanpoker.core.deck.Card
import com.lanpoker.core.deck.Rank
import com.lanpoker.core.deck.Suit
import java.util.concurrent.ConcurrentHashMap

/**
 * 真实牌面图片（assets 目录下的 png 牌面，来自 hayeah/playing-cards-assets 开源资源）。
 * 加载失败返回 null，调用方回退到矢量绘制。
 */
object CardImages {
    private val cache = ConcurrentHashMap<String, Bitmap>()

    fun load(context: Context, name: String): Bitmap? {
        return cache.getOrPut(name) {
            try {
                context.assets.open("cards/$name.png").use { BitmapFactory.decodeStream(it) }
            } catch (e: Exception) {
                null
            }
        }
    }

    fun fileNameFor(card: Card): String? = when (card) {
        is Card.Poker -> {
            val rank = when (card.rank) {
                Rank.ACE -> "ace"
                Rank.KING -> "king"
                Rank.QUEEN -> "queen"
                Rank.JACK -> "jack"
                Rank.TEN -> "10"
                else -> card.rank.label
            }
            val suit = when (card.suit) {
                Suit.SPADE -> "spades"
                Suit.HEART -> "hearts"
                Suit.CLUB -> "clubs"
                Suit.DIAMOND -> "diamonds"
            }
            "${rank}_of_$suit"
        }
        is Card.Joker -> if (card.big) "red_joker" else "black_joker"
    }
}
