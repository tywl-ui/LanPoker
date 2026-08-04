package com.lanpoker.core.config

enum class GameType(val label: String) {
    ZJH("炸金花"),
    DDZ("斗地主"),
}

/**
 * 公共配置：所有玩法共享。
 * 校验规则见 validate()，UI 配置页在开始前调用。
 */
data class GameConfig(
    val gameType: GameType = GameType.ZJH,
    val deckCount: Int = 1,
    val playerCount: Int = 4,
    val jokerCount: Int = 0,
    val baseScore: Int = 1,
) {
    val totalCards: Int get() = 52 * deckCount + jokerCount

    /** 返回错误信息，null 表示配置合法 */
    fun validate(): String? = when (gameType) {
        GameType.ZJH -> when {
            deckCount !in 1..3 -> "副数需在 1-3 之间"
            jokerCount !in 0..deckCount * 2 -> "王数量需在 0-${deckCount * 2} 之间（$deckCount 副）"
            playerCount !in 2..8 -> "人数需在 2-8 之间"
            totalCards < playerCount * 3 -> "牌数（$totalCards）不足以给 $playerCount 人各发 3 张"
            else -> null
        }
        GameType.DDZ -> when {
            deckCount !in 1..3 -> "副数需在 1-3 之间"
            jokerCount !in 0..deckCount * 2 -> "王数量需在 0-${deckCount * 2} 之间（$deckCount 副）"
            playerCount !in 3..6 -> "人数需在 3-6 之间"
            totalCards < playerCount * 3 + 3 -> "牌数不足：$playerCount 人 + 底牌至少需要 ${playerCount * 3 + 3} 张"
            else -> null
        }
    }
}
