package com.lanpoker.app.ui.config

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lanpoker.core.config.GameConfig
import com.lanpoker.core.config.GameType
import com.lanpoker.core.zjh.TieRule
import com.lanpoker.core.zjh.ZjhRules

/** 游戏模式 */
enum class GameMode(val label: String, val desc: String) {
    FRIENDS("好友局", "一台手机轮流操作，适合聚会面对面玩"),
    VS_AI_QUICK("人机快局", "固定倍数（闷1底/看2底），连开多把，AI 陪打"),
    VS_AI_FULL("人机标准局", "完整闷/看/跟/加/比/弃下注，AI 陪打"),
}

@Composable
fun ConfigScreen(
    onStart: (GameConfig, GameMode, Int, List<String>, ZjhRules, TieRule) -> Unit,
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
    var names by remember { mutableStateOf(listOf("玩家1", "玩家2", "玩家3", "玩家4")) }
    var rule235 by remember { mutableStateOf(true) }
    var tieRule by remember { mutableStateOf(TieRule.REDEAL) }

    val config = GameConfig(gameType, deckCount, playerCount, jokerCount, baseScore)
    val error = config.validate()
    val maxAi = playerCount - 1

    fun onPlayerCountChanged(newCount: Int) {
        playerCount = newCount
        names = List(newCount) { i -> names.getOrElse(i) { "玩家${i + 1}" } }
    }

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
            val selected = mode == m
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
            ) {
                Column(
                    modifier = Modifier
                        .clickable { mode = m }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(m.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(m.desc, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("对局配置", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))

        StepperRow("牌副数", deckCount, { deckCount = it }, 1..3)
        StepperRow("人数", playerCount, ::onPlayerCountChanged, 2..6)
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

        Spacer(Modifier.height(20.dp))
        Text("玩家名称", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        names.forEachIndexed { i, name ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                Text(
                    if (mode != GameMode.FRIENDS && i < aiCount) "AI 座" else "座${i + 1}",
                    modifier = Modifier.width(44.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { newName ->
                        names = names.toMutableList().also { it[i] = newName }
                    },
                    label = { Text("名称（可空）") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (mode != GameMode.FRIENDS && aiCount > 0) {
            Text(
                "前 $aiCount 个座位为 AI 对手，其余为真人（共用手机轮流操作）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(20.dp))
        Text("规则设置", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("杂色 235 吃豹子", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = rule235, onCheckedChange = { rule235 = it })
        }
        Spacer(Modifier.height(4.dp))
        Text("平局处理", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TieRule.values().forEach { r ->
                FilterChip(
                    selected = tieRule == r,
                    onClick = { tieRule = r },
                    label = { Text(r.label) },
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("规则速览", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                RuleLine("底注", "开局每人自动下 1 底；闷牌跟注 = level 底，看牌 = 2×level 底")
                RuleLine("加注", "倍数必须高于当前，可自填；看牌加注按 2 倍计")
                RuleLine("比牌", "三家以上只能与已看牌者比；剩两家可与闷牌者开牌；平局发起者输")
                RuleLine("牌型", "豹子 > 顺金 > 金花 > 顺子 > 对子 > 单张${if (rule235) "；杂色 235 吃豹子" else ""}")
            }
        }

        Spacer(Modifier.height(24.dp))
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = {
                onStart(
                    config,
                    mode,
                    aiCount,
                    names.map { it.trim() },
                    ZjhRules(rule235EatsTriple = rule235),
                    tieRule,
                )
            },
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
private fun RuleLine(label: String, text: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.primary,
        ) {
            Text(
                label,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
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
