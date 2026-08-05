package com.lanpoker.app.ui.zjh

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.lanpoker.app.sound.SoundFx
import com.lanpoker.app.ui.common.ActionBanner
import com.lanpoker.app.ui.common.Avatar
import com.lanpoker.app.ui.common.CardBack
import com.lanpoker.app.ui.common.CardFront
import com.lanpoker.app.ui.common.ChipStack
import com.lanpoker.app.ui.common.CountdownRing
import com.lanpoker.app.ui.common.Gold
import com.lanpoker.app.ui.common.TableBackground
import com.lanpoker.core.config.GameConfig
import com.lanpoker.core.deck.Card
import com.lanpoker.core.deck.Suit
import com.lanpoker.core.ledger.Player
import com.lanpoker.core.zjh.ZjhBettingGame
import com.lanpoker.core.zjh.ZjhEvaluator
import com.lanpoker.core.zjh.ZjhRules
import kotlinx.coroutines.delay

/**
 * 座位布局：0 号为当前行动者（屏幕下方正中），其余按人数分布在牌桌周围。
 * 位置为相对宽高的比例坐标。
 */
private fun slotsFor(total: Int): List<androidx.compose.ui.geometry.Offset> = when (total) {
    2 -> listOf(Offset(0.5f, 0.80f), Offset(0.5f, 0.12f))
    3 -> listOf(Offset(0.5f, 0.80f), Offset(0.12f, 0.28f), Offset(0.88f, 0.28f))
    4 -> listOf(Offset(0.5f, 0.80f), Offset(0.5f, 0.10f), Offset(0.12f, 0.32f), Offset(0.88f, 0.32f))
    5 -> listOf(
        Offset(0.5f, 0.80f), Offset(0.5f, 0.10f), Offset(0.5f, 0.34f),
        Offset(0.12f, 0.34f), Offset(0.88f, 0.34f),
    )
    else -> listOf(
        Offset(0.5f, 0.80f), Offset(0.5f, 0.08f),
        Offset(0.24f, 0.12f), Offset(0.76f, 0.12f),
        Offset(0.10f, 0.38f), Offset(0.90f, 0.38f),
    )
}

@Composable
fun ZjhGameScreen(
    config: GameConfig,
    aiIds: Set<Int>,
    aiEngine: AiEngine?,
    names: List<String>,
    rules: ZjhRules,
    onExit: () -> Unit,
    viewModel: ZjhGameViewModel = viewModel(factory = ZjhGameViewModel.factory(config, aiIds, aiEngine, names, rules)),
) {
    val state = viewModel.state
    val game = state.game
    var showBill by remember { mutableStateOf(false) }
    var showRaise by remember { mutableStateOf(false) }
    var showCompare by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }

    // 音效：发牌 / 动作 / 结算
    LaunchedEffect(state.round) { SoundFx.play("deal") }
    LaunchedEffect(state.game.state.lastAction) {
        val action = state.game.state.lastAction
        SoundFx.playForAction(action)
    }
    LaunchedEffect(state.phase) {
        if (state.phase == Phase.SETTLED) SoundFx.play("win")
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
    if (showCompare && state.phase == Phase.BETTING && state.pendingCompare == null) {
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
    state.pendingCompare?.let { pc ->
        val targetIdx = viewModel.players.indexOfFirst { it.id == pc.targetId }
        TransformDialog(
            title = if (pc.isTargetPick) "对方有王：选一个能赢过的牌型" else "你有王：先选你的牌型",
            options = pc.options,
            isTargetPick = pc.isTargetPick,
            realHand = if (pc.isTargetPick && targetIdx >= 0) game.hands.getOrNull(targetIdx) else null,
            onPick = viewModel::onTransformPicked,
            onCancel = viewModel::cancelPendingCompare,
        )
    }

    // 比牌对决特效
    val compareEvent = state.game.state.lastCompare
    LaunchedEffect(compareEvent) {
        if (compareEvent != null) {
            delay(3200)
            viewModel.dismissCompareShowdown()
        }
    }

    // 真人回合倒计时由 ViewModel 统一管理（AI 回合也有思考倒计时）
    val paused = state.pendingCompare != null || compareEvent != null
    if (compareEvent != null && state.phase == Phase.BETTING) {
        val challenger = viewModel.players.firstOrNull { it.id == compareEvent.challengerId }
        val target = viewModel.players.firstOrNull { it.id == compareEvent.targetId }
        val cIdx = game.players.indexOfFirst { it.id == compareEvent.challengerId }
        val tIdx = game.players.indexOfFirst { it.id == compareEvent.targetId }
        if (challenger != null && target != null && cIdx >= 0 && tIdx >= 0) {
            ShowdownOverlay(
                event = compareEvent,
                challenger = challenger,
                target = target,
                challengerCards = game.hands[cIdx],
                targetCards = game.hands[tIdx],
            )
        }
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
                TextButton(onClick = { showExitConfirm = true }) { Text("退出", color = Color.White) }
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
                ActionBanner(
                    action = state.game.state.lastAction,
                    key = state.game.state.lastAction,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
                if (state.phase == Phase.SETTLED) {
                    SettledPanel(
                        result = state.lastResult!!,
                        game = game,
                        players = viewModel.players,
                        scores = state.scores,
                        onNext = viewModel::nextRound,
                        onBill = { showBill = true },
                    )
                } else {
                    TableArea(
                        game = game,
                        players = viewModel.players,
                        aiIds = aiIds,
                        showMyCards = state.showMyCards,
                        turnSecondsLeft = viewModel.turnSecondsLeft,
                        round = state.round,
                    )
                }
            }

            ActionBar(
                state = state,
                players = viewModel.players,
                aiIds = aiIds,
                showMyCards = state.showMyCards,
                secondsLeft = viewModel.turnSecondsLeft,
                paused = paused,
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
    aiIds: Set<Int>,
    showMyCards: Boolean,
    turnSecondsLeft: Int,
    round: Int,
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
        val slots = slotsFor(players.size)

        players.forEach { p ->
            val slotIndex = if (p.id == actor) 0 else others.indexOf(p) + 1
            val slot = slots.getOrElse(slotIndex) { slots.first() }
            val targetX = (w * slot.x - seatW / 2).coerceAtLeast(0.dp)
            val targetY = (h * slot.y - seatH / 2).coerceAtLeast(0.dp)
            // 换位平滑滑动
            val animX by animateDpAsState(targetValue = targetX, animationSpec = tween(450), label = "seatX")
            val animY by animateDpAsState(targetValue = targetY, animationSpec = tween(450), label = "seatY")
            val isActorRevealed = p.id == actor && showMyCards
            key(round) {
                Seat(
                    player = p,
                    game = game,
                    isTurn = p.id == actor && !gs.over,
                    showCards = isActorRevealed,
                    cardW = if (isActorRevealed) cardW + 10.dp else cardW,
                    cardH = if (isActorRevealed) cardH + 14.dp else cardH,
                    overlap = overlap,
                    modifier = Modifier.offset(x = animX, y = animY),
                )
            }
        }

        // 桌子中心：底池 + 倒计时环
        val turnIsAi = gs.turn in aiIds
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ChipStack(pot = game.pot())
                    Text("底池 ${game.pot()} 分", color = Gold, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "第 ${gs.bettingRounds + 1} 轮下注${if (!game.canCompare()) " · 比牌需满3轮" else ""}",
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Spacer(Modifier.width(14.dp))
                if (!gs.over) {
                    CountdownRing(
                        secondsLeft = turnSecondsLeft,
                        total = if (turnIsAi) 5 else 20,
                        label = if (turnIsAi) "AI 思考" else "你的回合",
                        size = 62.dp,
                    )
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
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(280)) + scaleIn(initialScale = 0.6f, animationSpec = tween(280)),
        ) {
            Row {
                hand.forEachIndexed { i, card ->
                    val m = Modifier.offset(x = if (i == 0) 0.dp else overlap)
                    val showFace = showCards && !folded
                    Crossfade(
                        targetState = showFace,
                        animationSpec = tween(260),
                        label = "flip",
                    ) { face ->
                        if (face) {
                            CardFront(card = card, width = cardW, height = cardH, modifier = m)
                        } else {
                            CardBack(width = cardW, height = cardH, dimmed = folded, modifier = m)
                        }
                    }
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
    secondsLeft: Int,
    paused: Boolean,
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
                    if (!paused) {
                        Text(
                            "剩余 ${secondsLeft} 秒自动跟注",
                            color = if (secondsLeft <= 5) Color(0xFFFF8A80) else Color.White.copy(alpha = 0.7f),
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
                            Text(if (needPay) "跟注 ${req}底（${req * state.game.base}分）" else "过")
                        }
                        OutlinedButton(onClick = onRaise, border = BorderStroke(1.dp, Color.White)) {
                            Text("加注", color = Color.White)
                        }
                        val roundsLeft = state.game.roundsToCompare()
                        OutlinedButton(
                            onClick = onCompare,
                            enabled = roundsLeft == 0,
                            border = BorderStroke(1.dp, Color.White),
                        ) {
                            Text(
                                if (roundsLeft > 0) "比牌（${roundsLeft}轮后）" else "比牌",
                                color = if (roundsLeft > 0) Color.White.copy(alpha = 0.45f) else Color.White,
                            )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettledPanel(
    result: com.lanpoker.core.ledger.RoundResult,
    game: ZjhBettingGame,
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
        Spacer(Modifier.height(12.dp))
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
        result.winnerId?.let { wid ->
            val idx = game.players.indexOfFirst { it.id == wid }
            if (idx >= 0) {
                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1B5E20),
                    border = BorderStroke(2.dp, Gold),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        game.hands[idx].forEach { card ->
                            CardFront(card = card, width = 44.dp, height = 62.dp)
                        }
                    }
                }
            }
        }
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
                                    "${p.name} ${signed(scores[p.id] ?: 0)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
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

// ---------- 比牌对决特效 ----------

@Composable
private fun ShowdownOverlay(
    event: ZjhBettingGame.CompareEvent,
    challenger: Player,
    target: Player,
    challengerCards: List<Card>,
    targetCards: List<Card>,
) {
    var showResult by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(1200); showResult = true }

    val winnerId = if (event.loserId == event.challengerId) event.targetId else event.challengerId
    val challengerWon = winnerId == event.challengerId

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE60A2415)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(40.dp))
            // 对方（上方）
            ShowdownSide(
                name = "${target.name}（${event.targetLabel}）",
                cards = targetCards,
                isWinner = !challengerWon,
                showResult = showResult,
            )
            Spacer(Modifier.weight(1f))
            // VS 冲击特效
            VsEmblem(showResult = showResult)
            Spacer(Modifier.weight(1f))
            // 自己（下方桌面）
            ShowdownSide(
                name = "${challenger.name}（${event.challengerLabel}）",
                cards = challengerCards,
                isWinner = challengerWon,
                showResult = showResult,
            )
            Spacer(Modifier.height(48.dp))
        }
        // 胜负横幅
        if (showResult) {
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(animationSpec = tween(400)) { it } + fadeIn(tween(400)),
                modifier = Modifier.align(Alignment.Center),
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (challengerWon) Color(0xFF2E7D32) else Color(0xFFC62828),
                    border = BorderStroke(2.dp, Gold),
                    shadowElevation = 10.dp,
                ) {
                    Text(
                        if (challengerWon) "${challenger.name} 赢！" else "${target.name} 赢！",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ShowdownSide(
    name: String,
    cards: List<Card>,
    isWinner: Boolean,
    showResult: Boolean,
) {
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isWinner && showResult) Color(0x662E7D32) else Color(0x22000000),
            border = if (isWinner && showResult) BorderStroke(2.dp, Gold) else null,
        ) {
            Text(
                name,
                color = Color.White,
                fontWeight = if (showResult) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            cards.forEachIndexed { i, card ->
                AnimatedVisibility(
                    visible = revealed,
                    enter = fadeIn(tween(300)) +
                        scaleIn(initialScale = 0.3f, animationSpec = tween(350, delayMillis = i * 130)),
                ) {
                    Box(
                        modifier = Modifier.alpha(if (showResult && !isWinner) 0.35f else 1f),
                    ) {
                        CardFront(card = card, width = 58.dp, height = 82.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun VsEmblem(showResult: Boolean) {
    val pulse = rememberInfiniteTransition(label = "vs")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (showResult) 0.9f else 1.18f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "vsScale",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(92.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        // 光晕
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Gold.copy(alpha = 0.5f), Color.Transparent),
                    ),
                    CircleShape,
                ),
        )
        Text(
            "VS",
            fontSize = 40.sp,
            fontWeight = FontWeight.Black,
            color = Gold,
            style = MaterialTheme.typography.headlineLarge,
        )
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
private fun TransformDialog(
    title: String,
    options: List<com.lanpoker.core.zjh.ZjhHand>,
    isTargetPick: Boolean,
    realHand: List<Card>?,
    onPick: (com.lanpoker.core.zjh.ZjhHand) -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isTargetPick) onCancel() },
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (realHand != null) {
                    Text(
                        "你的手牌：",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        realHand.forEach { card ->
                            CardFront(card = card, width = 36.dp, height = 50.dp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    "王可以临时变成其他牌，选一个定型",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                options.forEach { h ->
                    Button(
                        onClick = { onPick(h) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                    ) { Text(ZjhEvaluator.optionLabel(h)) }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            if (!isTargetPick) {
                TextButton(onClick = onCancel) { Text("取消") }
            }
        },
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
