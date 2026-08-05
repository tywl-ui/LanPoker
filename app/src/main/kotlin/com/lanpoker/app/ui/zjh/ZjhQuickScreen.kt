package com.lanpoker.app.ui.zjh

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lanpoker.app.ai.AiEngine
import com.lanpoker.app.sound.SoundFx
import com.lanpoker.app.ui.common.ActionBanner
import com.lanpoker.app.ui.common.Avatar
import com.lanpoker.app.ui.common.CardBack
import com.lanpoker.app.ui.common.CardFront
import com.lanpoker.app.ui.common.CountdownRing
import com.lanpoker.app.ui.common.Gold
import com.lanpoker.app.ui.common.TableBackground
import com.lanpoker.core.config.GameConfig
import com.lanpoker.core.ledger.Player
import com.lanpoker.core.zjh.TieRule
import com.lanpoker.core.zjh.ZjhQuickGame
import com.lanpoker.core.zjh.ZjhRules
import kotlinx.coroutines.delay

private fun quickSlotsFor(total: Int): List<androidx.compose.ui.geometry.Offset> = when (total) {
    2 -> listOf(Offset(0.5f, 0.82f), Offset(0.5f, 0.12f))
    3 -> listOf(Offset(0.5f, 0.82f), Offset(0.12f, 0.28f), Offset(0.88f, 0.28f))
    4 -> listOf(Offset(0.5f, 0.82f), Offset(0.5f, 0.10f), Offset(0.12f, 0.32f), Offset(0.88f, 0.32f))
    5 -> listOf(
        Offset(0.5f, 0.82f), Offset(0.5f, 0.10f), Offset(0.5f, 0.34f),
        Offset(0.12f, 0.34f), Offset(0.88f, 0.34f),
    )
    else -> listOf(
        Offset(0.5f, 0.82f), Offset(0.5f, 0.08f),
        Offset(0.24f, 0.12f), Offset(0.76f, 0.12f),
        Offset(0.10f, 0.38f), Offset(0.90f, 0.38f),
    )
}

@Composable
fun ZjhQuickScreen(
    config: GameConfig,
    aiIds: Set<Int>,
    aiEngine: AiEngine,
    names: List<String>,
    rules: ZjhRules,
    tieRule: TieRule,
    onExit: () -> Unit,
    viewModel: ZjhQuickViewModel = viewModel(
        factory = ZjhQuickViewModel.factory(config, aiIds, aiEngine, names, rules, tieRule),
    ),
) {
    val state = viewModel.state
    val game = state.game
    var showBill by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }

    // 音效：发牌 / 选择 / 结算
    LaunchedEffect(state.round) { SoundFx.play("deal") }
    LaunchedEffect(state.game.state.chosen.size) {
        if (state.game.state.chosen.isNotEmpty()) SoundFx.play("tick")
    }
    LaunchedEffect(state.phase) {
        if (state.phase == QuickPhase.REVEAL) SoundFx.play("win")
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("退出对局？") },
            text = { Text("退出后本局账目将丢失，确定退出吗？") },
            confirmButton = {
                TextButton(onClick = { onExit() }) { Text("退出", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showExitConfirm = false }) { Text("继续玩") } },
        )
    }

    if (showBill) {
        BillDialogQuick(text = viewModel.exportBill(), onDismiss = { showBill = false })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .background(Color(0xFF0B3D24)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { showExitConfirm = true }) { Text("退出", color = Color.White) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "人机快局 · 闷${config.baseScore}底/看${config.baseScore * 2}底",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "第 ${state.round} 局 · ${config.deckCount}副 · ${config.playerCount}人",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(onClick = { showBill = true }) { Text("账单", color = Color.White) }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                TableBackground(modifier = Modifier.fillMaxSize())
                ActionBanner(
                    action = state.lastAction,
                    key = state.lastAction,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
                if (state.phase == QuickPhase.REVEAL) {
                    QuickSettledPanel(
                        state = state,
                        players = viewModel.players,
                        onNext = viewModel::nextRound,
                        onBill = { showBill = true },
                    )
                } else {
                    QuickTable(
                        game,
                        viewModel.players,
                        aiIds,
                        state.showMyCards,
                        viewModel.turnSecondsLeft,
                    )
                }
            }

            QuickActionBar(
                state = state,
                aiIds = aiIds,
                secondsLeft = viewModel.turnSecondsLeft,
                onChoose = viewModel::choose,
            )
        }
    }
}

@Composable
private fun QuickTable(
    game: ZjhQuickGame,
    players: List<Player>,
    aiIds: Set<Int>,
    showMyCards: Boolean,
    turnSecondsLeft: Int,
) {
    val gs = game.state
    val chooser = game.currentChooser

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        val compact = w < 400.dp
        val seatW = if (compact) 100.dp else 116.dp
        val seatH = if (compact) 150.dp else 160.dp
        val cardW = if (compact) 30.dp else 36.dp
        val cardH = if (compact) 42.dp else 50.dp
        val overlap = if (compact) (-12).dp else (-14).dp

        players.forEach { p ->
            val slotIndex = if (p.id == chooser?.id) 0 else {
                val idx = players.filter { it.id != chooser?.id }.indexOf(p)
                if (idx >= 0) idx + 1 else 0
            }
            val slot = quickSlotsFor(players.size).getOrElse(slotIndex) { quickSlotsFor(players.size).first() }
            val targetX = (w * slot.x - seatW / 2).coerceAtLeast(0.dp)
            val targetY = (h * slot.y - seatH / 2).coerceAtLeast(0.dp)
            // 换位平滑滑动
            val animX by animateDpAsState(targetValue = targetX, animationSpec = tween(450), label = "seatX")
            val animY by animateDpAsState(targetValue = targetY, animationSpec = tween(450), label = "seatY")
            QuickSeat(
                player = p,
                game = game,
                isChooser = p.id == chooser?.id,
                showCards = p.id == chooser?.id && showMyCards,
                cardW = cardW,
                cardH = cardH,
                overlap = overlap,
                modifier = Modifier.offset(x = animX, y = animY),
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "固定倍数 · 选完自动开牌",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            if (!game.allChosen) {
                val chooser = game.currentChooser
                val isAi = chooser != null && chooser.id in aiIds
                CountdownRing(
                    secondsLeft = turnSecondsLeft,
                    total = if (isAi) 5 else 15,
                    label = if (isAi) "AI 思考" else "你的回合",
                    size = 62.dp,
                )
            }
        }
    }
}

@Composable
private fun QuickSeat(
    player: Player,
    game: ZjhQuickGame,
    isChooser: Boolean,
    showCards: Boolean,
    cardW: Dp,
    cardH: Dp,
    overlap: Dp,
    modifier: Modifier = Modifier,
) {
    val gs = game.state
    val stake = gs.chosen[player.id]
    val hand = game.hands[game.players.indexOf(player)]

    Column(modifier = modifier.width(116.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Avatar(name = player.name, index = game.players.indexOf(player), isTurn = isChooser)
        Text(
            player.name,
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isChooser) FontWeight.Bold else FontWeight.Normal,
        )
        Spacer(Modifier.height(4.dp))
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(280)) + scaleIn(initialScale = 0.6f, animationSpec = tween(280)),
        ) {
            Row {
                hand.forEachIndexed { i, card ->
                    val m = Modifier.offset(x = if (i == 0) 0.dp else overlap)
                    Crossfade(
                        targetState = showCards,
                        animationSpec = tween(260),
                        label = "flip",
                    ) { face ->
                        if (face) {
                            CardFront(card = card, width = cardW, height = cardH, modifier = m)
                        } else {
                            CardBack(width = cardW, height = cardH, modifier = m)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (stake != null) Color(0xFF43A047) else Color(0x99000000),
        ) {
            Text(
                when {
                    stake == null -> "未选"
                    stake == game.base -> "闷 ${game.base}底"
                    else -> "看 ${game.base * 2}底"
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickActionBar(
    state: QuickUiState,
    aiIds: Set<Int>,
    secondsLeft: Int,
    onChoose: (Boolean) -> Unit,
) {
    val chooser = state.game.currentChooser
    Surface(color = Color(0xFF0B3D24), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            when {
                state.phase == QuickPhase.REVEAL -> Text(
                    "本局已结算",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                )
                chooser == null -> Text("开牌中…", color = Color.White)
                state.showMyCards -> Text(
                    "看牌中…（牌展示给你看，稍后自动传给下一位）",
                    color = Gold,
                    style = MaterialTheme.typography.titleSmall,
                )
                chooser.id in aiIds -> Text(
                    "${chooser.name} 思考中…",
                    color = Gold,
                    style = MaterialTheme.typography.titleSmall,
                )
                else -> {
                    Text(
                        "轮到你：${chooser.name}（每人固定倍数，选完自动亮牌）",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        "剩余 ${secondsLeft} 秒自动闷牌",
                        color = Color(0xFFFF8A80),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Button(
                            onClick = { onChoose(false) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                            modifier = Modifier.weight(1f),
                        ) { Text("闷牌 · 下 ${state.game.base} 底") }
                        Button(
                            onClick = { onChoose(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            modifier = Modifier.weight(1f),
                        ) { Text("看牌 · 下 ${state.game.base * 2} 底") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickSettledPanel(
    state: QuickUiState,
    players: List<Player>,
    onNext: () -> Unit,
    onBill: () -> Unit,
) {
    val result = state.lastResult!!
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            if (result.isDraw) "本局和局（牌型相同）" else "${result.winnerName} 赢！",
            color = Gold,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        result.winnerHandLabel?.let {
            Text("牌型：$it", color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(16.dp))
        // 全员亮牌
        state.game.players.forEach { p ->
            val hand = state.game.hands[state.game.players.indexOf(p)]
            val isWinner = p.id == result.winnerId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isWinner) Color(0xFF2E7D32) else Color(0x99000000),
                    modifier = Modifier.padding(4.dp),
                ) {
                    Text(
                        p.name,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                    )
                }
                Spacer(Modifier.width(8.dp))
                hand.forEach { CardFront(card = it, width = 40.dp, height = 56.dp, modifier = Modifier.offset(x = 0.dp)) }
                Spacer(Modifier.weight(1f))
                val d = result.deltas[p.id] ?: 0
                Text(
                    signed(d),
                    color = if (d >= 0) Color(0xFF81C784) else Color(0xFFEF9A9A),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Surface(color = Color(0xFFF1F8E9), shape = RoundedCornerShape(8.dp)) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("总分", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    players.forEach { p ->
                        Text(
                            "${p.name} ${signed(state.scores[p.id] ?: 0)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onNext) { Text("下一局") }
            OutlinedButton(onClick = onBill, border = BorderStroke(1.dp, Color.White)) { Text("账单", color = Color.White) }
        }
    }
}

@Composable
private fun BillDialogQuick(text: String, onDismiss: () -> Unit) {
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

private fun signed(v: Int) = if (v >= 0) "+$v" else "$v"
