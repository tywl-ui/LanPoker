package com.lanpoker.app.ui.zjh

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import com.lanpoker.core.config.GameConfig
import com.lanpoker.core.deck.Card
import com.lanpoker.core.deck.Suit
import com.lanpoker.core.zjh.ZjhEvaluator

private val RED = Color(0xFFD32F2F)
private val BLACK = Color(0xFF212121)
private val MULTIPLIER_CHOICES = listOf(1, 2, 3, 5, 10, 20)

@Composable
fun ZjhGameScreen(
    config: GameConfig,
    onExit: () -> Unit,
    viewModel: ZjhGameViewModel = viewModel(factory = ZjhGameViewModel.factory(config)),
) {
    val state = viewModel.state
    var showBill by remember { mutableStateOf(false) }

    if (showBill) {
        BillDialog(text = viewModel.exportBill(), onDismiss = { showBill = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onExit) { Text("退出") }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "炸金花 · ${config.deckCount}副 · ${config.playerCount}人 · 底分${config.baseScore}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "第 ${(viewModel.state.lastResult?.round ?: 0) + 1} 局",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        when (state.phase) {
            Phase.PASS_PHONE -> PassPhoneContent(state, viewModel::confirmViewed)
            Phase.REVEAL -> RevealContent(state, viewModel::startMultiplier)
            Phase.MULTIPLIER -> MultiplierContent(state, viewModel::setMultiplier, viewModel::settle)
            Phase.SETTLED -> SettledContent(state, viewModel::deal, { showBill = true })
        }

        Spacer(Modifier.height(16.dp))
        ScoreBar(viewModel.players, state.scores)
    }
}

@Composable
private fun PassPhoneContent(
    state: UiState,
    onConfirm: () -> Unit,
) {
    val viewer = state.currentViewer
    val hand = state.hands.getOrNull(viewer) ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        state.message?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Text(
            "轮到你啦：${viewModelPlayerName(state, viewer)}",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            "其他人请别看屏幕，看完把手机传下去",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            hand.forEach { CardView(it, faceUp = true) }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "第 ${viewer + 1} / ${state.hands.size} 位",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = onConfirm) {
            Text(if (viewer == state.hands.size - 1) "全部看完，亮牌" else "看好了，传给下一位")
        }
    }
}

@Composable
private fun RevealContent(
    state: UiState,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("亮牌！", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        state.hands.forEachIndexed { i, hand ->
            val isWinner = i == state.winnerIndex
            val evaluated = state.evaluated.getOrNull(i)
            Surface(
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = if (isWinner) 3.dp else 1.dp,
                    color = if (isWinner) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        viewModelPlayerName(state, i),
                        modifier = Modifier.width(64.dp),
                        fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                    )
                    Spacer(Modifier.width(8.dp))
                    hand.forEach { CardView(it, faceUp = true) }
                    Spacer(Modifier.weight(1f))
                    evaluated?.let {
                        Column(horizontalAlignment = Alignment.End) {
                            if (isWinner) {
                                Text(
                                    "赢家",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Text(
                                ZjhEvaluator.describe(it),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onNext) { Text("开始填倍数") }
    }
}

@Composable
private fun MultiplierContent(
    state: UiState,
    onSetMultiplier: (Int, Int) -> Unit,
    onSettle: () -> Unit,
) {
    val winnerIndex = state.winnerIndex ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            "赢家：${viewModelPlayerName(state, winnerIndex)}（${ZjhEvaluator.describe(state.evaluated[winnerIndex])}）",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "每个输家自己填：这局输几个底",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        state.hands.forEachIndexed { i, _ ->
            if (i == winnerIndex) return@forEachIndexed
            val playerId = i + 1
            val value = state.multipliers[playerId] ?: 0
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text("${viewModelPlayerName(state, i)} 输：", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    MULTIPLIER_CHOICES.forEach { choice ->
                        FilterChip(
                            selected = value == choice,
                            onClick = { onSetMultiplier(playerId, choice) },
                            label = { Text("$choice") },
                        )
                    }
                    OutlinedTextField(
                        value = if (value in MULTIPLIER_CHOICES) "" else value.toString(),
                        onValueChange = {
                            onSetMultiplier(playerId, it.toIntOrNull() ?: 0)
                        },
                        label = { Text("自填") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onSettle, modifier = Modifier.fillMaxWidth()) {
            Text("结算本局，自动记账")
        }
    }
}

@Composable
private fun SettledContent(
    state: UiState,
    onNext: () -> Unit,
    onBill: () -> Unit,
) {
    val result = state.lastResult ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            if (result.isDraw) "第 ${result.round} 局：和局" else "第 ${result.round} 局：${result.winnerName} 赢（${result.winnerHandLabel}）",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        result.entries.forEach { e ->
            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                Text("${e.playerName}：", modifier = Modifier.weight(1f))
                val text = if (e.delta >= 0) "+${e.delta}" else "${e.delta}"
                Text(
                    "$text 分",
                    color = if (e.delta >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onNext, modifier = Modifier.weight(1f)) {
                Text("下一局")
            }
            OutlinedButton(onClick = onBill, modifier = Modifier.weight(1f)) {
                Text("账单")
            }
        }
    }
}

@Composable
private fun ScoreBar(players: List<com.lanpoker.core.ledger.Player>, scores: Map<Int, Int>) {
    Column {
        Text("当前总分", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            players.forEach { p ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        "${p.name} ${signed(scores[p.id] ?: 0)}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun BillDialog(text: String, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("账单") },
        text = {
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                clipboard.setText(AnnotatedString(text))
                Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                onDismiss()
            }) { Text("复制") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

@Composable
private fun CardView(card: Card, faceUp: Boolean) {
    val color = when (card) {
        is Card.Poker -> if (card.suit == Suit.HEART || card.suit == Suit.DIAMOND) RED else BLACK
        is Card.Joker -> MaterialTheme.colorScheme.primary
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.size(width = 64.dp, height = 92.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                if (faceUp) card.label else "?",
                fontSize = if (faceUp) 20.sp else 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (faceUp) color else Color.Gray,
            )
        }
    }
}

private fun viewModelPlayerName(state: UiState, index: Int): String = "玩家${index + 1}"

private fun signed(v: Int) = if (v >= 0) "+$v" else "$v"
