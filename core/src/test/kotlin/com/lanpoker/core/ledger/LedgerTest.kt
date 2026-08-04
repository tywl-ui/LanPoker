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
    fun 按分数差记账() {
        val s = session()
        s.settleRound(winnerId = 1, winnerHandLabel = "豹子A", deltas = mapOf(1 to 6, 2 to -3, 3 to -1, 4 to -2))
        assertEquals(mapOf(1 to 6, 2 to -3, 3 to -1, 4 to -2), s.scores)
        assertEquals(1, s.rounds.size)
        assertEquals("张三", s.rounds[0].winnerName)
        assertEquals("豹子A", s.rounds[0].winnerHandLabel)
    }

    @Test
    fun 全零为和局() {
        val s = session()
        s.settleRound(winnerId = null, winnerHandLabel = null, deltas = mapOf(1 to 0, 2 to 0, 3 to 0, 4 to 0))
        assertEquals(mapOf(1 to 0, 2 to 0, 3 to 0, 4 to 0), s.scores)
        assertEquals(true, s.rounds[0].isDraw)
    }

    @Test
    fun 多局累计() {
        val s = session()
        s.settleRound(winnerId = 1, winnerHandLabel = "豹子A", deltas = mapOf(1 to 4, 2 to -2, 3 to -1, 4 to -1))
        s.settleRound(winnerId = 2, winnerHandLabel = "金花", deltas = mapOf(1 to -3, 2 to 5, 3 to -1, 4 to -1))
        assertEquals(mapOf(1 to 1, 2 to 3, 3 to -2, 4 to -2), s.scores)
    }

    @Test
    fun 导出账单() {
        val s = session()
        s.settleRound(winnerId = 1, winnerHandLabel = "豹子A", deltas = mapOf(1 to 6, 2 to -3, 3 to -1, 4 to -2))
        val text = s.exportText()
        assertEquals(true, text.contains("张三"))
        assertEquals(true, text.contains("豹子A"))
        assertEquals(true, text.contains("+6"))
        assertEquals(true, text.contains("-3"))
    }
}
