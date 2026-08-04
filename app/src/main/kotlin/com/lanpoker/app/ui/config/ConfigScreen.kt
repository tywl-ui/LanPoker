package com.lanpoker.app.ui.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lanpoker.core.config.GameConfig
import com.lanpoker.core.config.GameType

/** 游戏模式 */
enum class GameMode(val label: String, val desc: String) {
    FRIENDS("好友局", "一台手机轮流操作，适合聚会面对面玩"),
    VS_AI_QUICK("人机快局", "固定倍数（闷1底/看2底），连开多把，AI 陪打"),
    VS_AI_FULL("人机标准局", "完整闷/看/跟/加/比/弃下注，AI 陪打"),
}

@Composable
fun ConfigScreen(
    onStart: (GameConfig, GameMode, Int) -> Unit,
    onOpenAiSettings: () -> Unit,
) {
    var gameType by remember { mutableStateOf(GameType.ZJH) }
    var mode by remember { mutableStateOf(GameMode.FRIENDS) }
    var deckCount by remember { mutableIntStateOf(1) }
    var playerCount by remember { mutableIntStateOf(4) }
    var jokerCount by remember { mutableIntStateOf(0) }
    var baseScore by remember { mutableIntStateOf(1) }
    var aiCount by remember { mutableIntStateOf(3) }
    var ddzHint by remember { mutableStateOf(false) }

    val config = GameConfig(gameType, deckCount, playerCount, jokerCount, baseScore)
    val error = config.validate()
    val maxAi = playerCount - 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text("局域网棋牌", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("自由设置副数 / 人数 / 王，AI 陪打，自动记账", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))

        Text("玩法", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = gameType == GameType.ZJH,
                onClick = { gameType = GameType.ZJH },
                label = { Text("炸金花") },
            )
            FilterChip(
                selected = false,
                onClick = { ddzHint = true },
                label = { Text("斗地主（开发中）") },
            )
        }
        if (ddzHint) {
            Spacer(Modifier.height(4.dp))
            Text("斗地主引擎正在开发，敬请期待", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(20.dp))
        Text("模式", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        GameMode.values().forEach { m ->
            FilterChip(
                selected = mode == m,
                onClick = { mode = m },
                label = { Text("${m.label} · ${m.desc}") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
            )
        }

        Spacer(Modifier.height(20.dp))
        Text("对局配置", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))

        StepperRow("牌副数", deckCount, { deckCount = it }, 1..3)
        StepperRow("人数", playerCount, { playerCount = it }, 2..6)
        if (mode != GameMode.FRIENDS) {
            StepperRow("AI 人数", aiCount, { aiCount = it }, 1..maxAi)
        }
        StepperRow("王数量", jokerCount, { jokerCount = it }, 0..(deckCount * 2))
        StepperRow("底分", baseScore, { baseScore = it }, 1..20)

        Spacer(Modifier.height(12.dp))
        Text(
            "牌数合计：${config.totalCards} 张",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = { onStart(config, mode, aiCount) },
            enabled = error == null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("开始游戏", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onOpenAiSettings,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("AI 设置（配置自己的大模型 API）") }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
    range: IntRange,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        IconButton(
            onClick = { if (value > range.first) onChange(value - 1) },
            enabled = value > range.first,
        ) { Text("−", style = MaterialTheme.typography.titleLarge) }
        Text(
            "$value",
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
        IconButton(
            onClick = { if (value < range.last) onChange(value + 1) },
            enabled = value < range.last,
        ) { Text("＋", style = MaterialTheme.typography.titleLarge) }
    }
}
