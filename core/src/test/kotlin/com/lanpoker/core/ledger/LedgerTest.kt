package com.lanpoker.core.ledger

import com.lanpoker.core.config.GameConfig
import com.lanpoker.core.config.GameType
import org.junit.Test
import kotlin.test.assertEquals

class LedgerTest {

    private fun session() = LedgerSession(
        config = GameConfig(gameType = GameType.ZJH, playerCount = 4, baseScore = 1),
        players = listOf(Player(1, "张三"), Player(2, "李四"), Player(3, "王五"), Player(4, "赵六")),
    )

    @Test
    fun 输家按倍数扣分赢家收入为总和() {
        val s = session()
        s.settle(winnerId = 1, winnerHandLabel = "豹子A", multipliers = mapOf(2 to 3, 3 to 1, 4 to 2))
        assertEquals(mapOf(1 to 6, 2 to -3, 3 to -1, 4 to -2), s.scores)
    }

    @Test
    fun 倍数0不扣分() {
        val s = session()
        s.settle(winnerId = 1, winnerHandLabel = "豹子A", multipliers = mapOf(2 to 0, 3 to 0, 4 to 0))
        assertEquals(mapOf(1 to 0, 2 to 0, 3 to 0, 4 to 0), s.scores)
    }

    @Test
    fun 和局不记分() {
        val s = session()
        s.settle(winnerId = null, winnerHandLabel = null, multipliers = emptyMap())
        assertEquals(mapOf(1 to 0, 2 to 0, 3 to 0, 4 to 0), s.scores)
        assertEquals(1, s.rounds.size)
        assertEquals(true, s.rounds[0].isDraw)
    }

    @Test
    fun 多局累计() {
        val s = session()
        s.settle(winnerId = 1, winnerHandLabel = "豹子A", multipliers = mapOf(2 to 2, 3 to 1, 4 to 1))
        s.settle(winnerId = 2, winnerHandLabel = "金花", multipliers = mapOf(1 to 3, 3 to 1, 4 to 1))
        assertEquals(mapOf(1 to 1, 2 to 3, 3 to -2, 4 to -2), s.scores)
    }

    @Test
    fun 底分放大() {
        val s = LedgerSession(
            config = GameConfig(gameType = GameType.ZJH, playerCount = 3, baseScore = 10),
            players = listOf(Player(1, "张三"), Player(2, "李四"), Player(3, "王五")),
        )
        s.settle(winnerId = 1, winnerHandLabel = "顺金", multipliers = mapOf(2 to 2, 3 to 3))
        assertEquals(mapOf(1 to 50, 2 to -20, 3 to -30), s.scores)
    }

    @Test
    fun 导出账单() {
        val s = session()
        s.settle(winnerId = 1, winnerHandLabel = "豹子A", multipliers = mapOf(2 to 3, 3 to 1, 4 to 2))
        val text = s.exportText()
        assertEquals(true, text.contains("张三"))
        assertEquals(true, text.contains("豹子A"))
        assertEquals(true, text.contains("+6"))
        assertEquals(true, text.contains("-3"))
    }
}
