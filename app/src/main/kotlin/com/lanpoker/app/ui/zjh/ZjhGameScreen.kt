package com.lanpoker.app.ui.zjh

import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lanpoker.app.ai.AiEngine
import com.lanpoker.app.ui.common.Avatar
import com.lanpoker.app.ui.common.CardBack
import com.lanpoker.app.ui.common.CardFront
import com.lanpoker.app.ui.common.ChipStack
import com.lanpoker.app.ui.common.Gold
import com.lanpoker.app.ui.common.TableBackground
import com.lanpoker.core.config.GameConfig
import com.lanpoker.core.deck.Card
import com.lanpoker.core.deck.Suit
import com.lanpoker.core.ledger.Player
import com.lanpoker.core.zjh.ZjhBettingGame

private val SLOTS = listOf(
    androidx.compose.ui.geometry.Offset(0.5f, 0.80f),
    androidx.compose.ui.geometry.Offset(0.5f, 0.10f),
    androidx.compose.ui.geometry.Offset(0.10f, 0.26f),
    androidx.compose.ui.geometry.Offset(0.90f, 0.26f),
    androidx.compose.ui.geometry.Offset(0.10f, 0.56f),
    androidx.compose.ui.geometry.Offset(0.90f, 0.56f),
)

@Composable
fun ZjhGameScreen(
    config: GameConfig,
    aiIds: Set<Int>,
    aiEngine: AiEngine?,
    onExit: () -> Unit,
    viewModel: ZjhGameViewModel = viewModel(factory = ZjhGameViewModel.factory(config, aiIds, aiEngine)),
) {
    val state = viewModel.state
    val game = state.game
    var showBill by remember { mutableStateOf(false) }
    var showRaise by remember { mutableStateOf(false) }
    var showCompare by remember { mutableStateOf(false) }

    if (showBill) {
        BillDialog(text = viewModel.exportBill(), onDismiss = { showBill = false })
    }
    if (showRaise && state.phase == Phase.BETTING) {
        RaiseDialog(
            currentLevel = game.state.level,
            maxLevel = game.maxLevel,
            onPick = { viewModel.raise(it); showRaise = false },
            onDismiss = { showRaise = false },
        )
    }
    if (showCompare && state.phase == Phase.BETTING) {
        val active = viewModel.players.count { it.id !in game.state.folded }
        val eligible = if (active > 2) {
            // 三家以上只能与已看牌的玩家比牌
            viewModel.players.filter {
                it.id != game.state.turn && it.id !in game.state.folded && it.id in game.state.looked
            }
        } else {
            viewModel.players.filter { it.id != game.state.turn && it.id !in game.state.folded }
        }
        CompareDialog(
            targets = eligible,
            hint = if (active > 2) "三家以上只能和已看牌的玩家比牌" else "仅剩两家，可与任何对手开牌",
            onPick = { viewModel.compare(it); showCompare = false },
            onDismiss = { showCompare = false },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B3D24)),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onExit) { Text("退出", color = Color.White) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (aiIds.isEmpty()) "好友局 · 炸金花" else "人机标准局 · 炸金花",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "第 ${state.round} 局 · ${config.deckCount}副 · ${config.playerCount}人 · 底分 ${config.baseScore}",
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
                if (state.phase == Phase.SETTLED) {
                    SettledPanel(
                        result = state.lastResult!!,
                        players = viewModel.players,
                        scores = state.scores,
                        onNext = viewModel::nextRound,
                        onBill = { showBill = true },
                    )
                } else {
                    TableArea(
                        game = game,
                        players = viewModel.players,
                        showMyCards = state.showMyCards,
                    )
                }
            }

            ActionBar(
                state = state,
                players = viewModel.players,
                aiIds = aiIds,
                showMyCards = state.showMyCards,
                onLook = viewModel::look,
                onHide = viewModel::hideCards,
                onCall = viewModel::call,
                onRaise = { showRaise = true },
                onCompare = { showCompare = true },
                onFold = viewModel::fold,
            )
        }
    }
}

// ---------- 牌桌 ----------

@Composable
private fun TableArea(
    game: ZjhBettingGame,
    players: List<Player>,
    showMyCards: Boolean,
) {
    val gs = game.state
    val actor = gs.turn
    val others = players.filter { it.id != actor }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        val compact = w < 400.dp
        val seatW = if (compact) 100.dp else 116.dp
        val seatH = if (compact) 160.dp else 170.dp
        val cardW = if (compact) 30.dp else 36.dp
        val cardH = if (compact) 42.dp else 50.dp
        val overlap = if (compact) (-12).dp else (-14).dp

        players.forEach { p ->
            val slotIndex = if (p.id == actor) 0 else others.indexOf(p) + 1
            val slot = SLOTS[slotIndex]
            val x = (w * slot.x - seatW / 2).coerceAtLeast(0.dp)
            val y = (h * slot.y - seatH / 2).coerceAtLeast(0.dp)
            Seat(
                player = p,
                game = game,
                isTurn = p.id == actor && !gs.over,
                showCards = p.id == actor && showMyCards,
                cardW = cardW,
                cardH = cardH,
                overlap = overlap,
                modifier = Modifier.offset(x = x, y = y),
            )
        }

        // 桌子中心：底池
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF0B3D24).copy(alpha = 0.9f),
                border = BorderStroke(2.dp, Color(0xFF2E7D32)),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ChipStack(pot = game.pot())
                    Text("底池 ${game.pot()} 分", color = Gold, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(6.dp))
            gs.lastAction?.let {
                Text(
                    it,
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun Seat(
    player: Player,
    game: ZjhBettingGame,
    isTurn: Boolean,
    showCards: Boolean,
    cardW: Dp,
    cardH: Dp,
    overlap: Dp,
    modifier: Modifier = Modifier,
) {
    val gs = game.state
    val folded = player.id in gs.folded
    val looked = player.id in gs.looked
    val stake = gs.stakes[player.id] ?: 0
    val hand = game.hands[game.players.indexOf(player)]

    Column(modifier = modifier.width(116.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Avatar(name = player.name, index = game.players.indexOf(player), isTurn = isTurn)
        Text(
            player.name,
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isTurn) FontWeight.Bold else FontWeight.Normal,
        )
        Spacer(Modifier.height(4.dp))
        Row {
            hand.forEachIndexed { i, card ->
                val m = Modifier.offset(x = if (i == 0) 0.dp else overlap)
                if (showCards && !folded) {
                    CardFront(card = card, width = cardW, height = cardH, modifier = m)
                } else {
                    CardBack(width = cardW, height = cardH, dimmed = folded, modifier = m)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = when {
                folded -> Color(0x99616161)
                isTurn && !looked -> Color(0xFF1E88E5)
                looked -> Color(0xFF43A047)
                else -> Color(0xCCFFFFFF)
            },
        ) {
            Text(
                when {
                    folded -> "已弃牌"
                    isTurn && !looked -> "闷牌"
                    looked -> "已看牌"
                    else -> "等待中"
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        if (stake > 0 && !folded) {
            Text(
                "已投 $stake",
                color = Gold,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

// ---------- 底部操作栏 ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionBar(
    state: UiState,
    players: List<Player>,
    aiIds: Set<Int>,
    showMyCards: Boolean,
    onLook: () -> Unit,
    onHide: () -> Unit,
    onCall: () -> Unit,
    onRaise: () -> Unit,
    onCompare: () -> Unit,
    onFold: () -> Unit,
) {
    val gs = state.game.state
    val actor = players.firstOrNull { it.id == gs.turn }

    Surface(color = Color(0xFF0B3D24), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            if (state.phase == Phase.BETTING && actor != null) {
                if (actor.id in aiIds) {
                    Text(
                        "${actor.name} 思考中…",
                        color = Gold,
                        style = MaterialTheme.typography.titleSmall,
                    )
                } else {
                    val looked = gs.looked.contains(actor.id)
                    val stake = gs.stakes[actor.id] ?: 0
                    val req = if (looked) 2 * gs.level else gs.level
                    val needPay = stake < req * state.game.base
                    Text(
                        "轮到你：${actor.name}（${if (looked) "已看牌" else "闷牌"}）",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    if (needPay) {
                        Text(
                            "已投 $stake · 还需 ${req * state.game.base - stake} 分",
                            color = Gold,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (!looked) {
                            Button(
                                onClick = if (showMyCards) onHide else onLook,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                            ) { Text(if (showMyCards) "合上" else "看牌") }
                        }
                        Button(
                            onClick = onCall,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        ) {
                            Text(if (needPay) "跟注 ${req}底" else "过")
                        }
                        OutlinedButton(onClick = onRaise, border = BorderStroke(1.dp, Color.White)) {
                            Text("加注", color = Color.White)
                        }
                        OutlinedButton(onClick = onCompare, border = BorderStroke(1.dp, Color.White)) {
                            Text("比牌", color = Color.White)
                        }
                        Button(
                            onClick = onFold,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        ) { Text("弃牌") }
                    }
                }
            }
        }
    }
}

// ---------- 结算面板 ----------

@Composable
private fun SettledPanel(
    result: com.lanpoker.core.ledger.RoundResult,
    players: List<Player>,
    scores: Map<Int, Int>,
    onNext: () -> Unit,
    onBill: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            "${result.winnerName} 赢！",
            color = Gold,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "牌型：${result.winnerHandLabel}",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(16.dp))
        Surface(shape = RoundedCornerShape(12.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                result.entries.forEach { e ->
                    Row(modifier = Modifier.padding(vertical = 3.dp)) {
                        Text("${e.playerName}：", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            signed(e.delta),
                            color = if (e.delta >= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Surface(color = Color(0xFFF1F8E9), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        "总分：" + players.joinToString("  ") { "${it.name} ${signed(scores[it.id] ?: 0)}" },
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
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

// ---------- 对话框 ----------

@Composable
private fun RaiseDialog(
    currentLevel: Int,
    maxLevel: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val choices = listOf(2, 3, 5, 10).filter { it > currentLevel && it <= maxLevel }
    var custom by remember { mutableStateOf("") }
    val customValue = custom.toIntOrNull()
    val customValid = customValue != null && customValue > currentLevel && customValue <= maxLevel

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("加注到几倍？") },
        text = {
            Column {
                Text(
                    "规则：看牌出的倍数是闷牌的 2 倍；后一家的倍数不能低于当前（$currentLevel 倍）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                choices.forEach { level ->
                    Button(
                        onClick = { onPick(level) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                    ) { Text("${level} 倍（闷 $level 底 / 看 ${level * 2} 底）") }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it.filter(Char::isDigit).take(3) },
                    label = { Text("自填倍数") },
                    placeholder = { Text("大于 $currentLevel") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (custom.isNotBlank() && !customValid) {
                    Text(
                        "需大于当前 $currentLevel 倍且不超过 $maxLevel",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = customValid,
                onClick = { customValue?.let(onPick) },
            ) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun CompareDialog(
    targets: List<Player>,
    hint: String,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("和谁比牌？") },
        text = {
            Column {
                Text(
                    hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                if (targets.isEmpty()) Text("没有可比的玩家")
                targets.forEach { p ->
                    Button(
                        onClick = { onPick(p.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                    ) { Text("和 ${p.name} 比") }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

private fun signed(v: Int) = if (v >= 0) "+$v" else "$v"
