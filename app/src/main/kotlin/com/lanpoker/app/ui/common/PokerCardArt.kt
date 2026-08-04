package com.lanpoker.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lanpoker.core.deck.Card
import com.lanpoker.core.deck.Suit

val CardRed = Color(0xFFD32F2F)
val CardBlack = Color(0xFF1F1F1F)
val Gold = Color(0xFFFFD54F)

private val avatarPalette = listOf(
    Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFF8E24AA),
    Color(0xFFFB8C00), Color(0xFF00ACC1), Color(0xFFD81B60), Color(0xFF6D4C41),
)

fun avatarColor(index: Int): Color = avatarPalette[index % avatarPalette.size]

/** 牌面（矢量绘制） */
@Composable
fun CardFront(
    card: Card,
    modifier: Modifier = Modifier,
    width: Dp = 64.dp,
    height: Dp = 92.dp,
) {
    val color = when (card) {
        is Card.Poker -> if (card.suit == Suit.HEART || card.suit == Suit.DIAMOND) CardRed else CardBlack
        is Card.Joker -> Color(0xFF1E88E5)
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFBDBDBD)),
        modifier = modifier.size(width, height),
    ) {
        Box(contentAlignment = Alignment.Center) {
            // 角落索引
            val cornerMod = Modifier.offset(x = 4.dp, y = 2.dp)
            Box(modifier = Modifier.align(Alignment.TopStart).then(cornerMod)) {
                Column {
                    Text(
                        card.label,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        lineHeight = 16.sp,
                    )
                }
            }
            // 中央图案
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when (card) {
                    is Card.Poker -> {
                        val showRank = card.rank.value >= 11
                        if (showRank) {
                            Text(
                                card.rank.label,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = color,
                            )
                        }
                        Text(
                            card.suit.symbol,
                            fontSize = if (showRank) 22.sp else 34.sp,
                            color = color,
                            textAlign = TextAlign.Center,
                        )
                    }
                    is Card.Joker -> {
                        Text(
                            if (card.big) "大" else "小",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .background(color, CircleShape)
                                .padding(6.dp),
                        )
                    }
                }
            }
        }
    }
}

/** 牌背（花纹） */
@Composable
fun CardBack(
    modifier: Modifier = Modifier,
    width: Dp = 64.dp,
    height: Dp = 92.dp,
    dimmed: Boolean = false,
) {
    val base = if (dimmed) Color(0xFF757575) else Color(0xFF1565C0)
    val accent = if (dimmed) Color(0xFF9E9E9E) else Color(0xFF64B5F6)
    Box(
        modifier = modifier.size(width, height),
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = base,
            border = BorderStroke(1.dp, if (dimmed) Color(0xFF616161) else Color(0xFF0D47A1)),
            modifier = Modifier.size(width, height),
        ) {
            Canvas(modifier = Modifier.size(width, height)) {
                val w = size.width
                val h = size.height
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(base, base.copy(alpha = 0.75f)),
                    ),
                    size = size,
                )
                // 菱形格纹
                val step = 12.dp.toPx()
                val paintColor = accent.copy(alpha = 0.5f)
                var x = 0f
                while (x < w) {
                    var y = 0f
                    while (y < h) {
                        val path = Path().apply {
                            moveTo(x, y - step / 2)
                            lineTo(x + step / 2, y)
                            lineTo(x, y + step / 2)
                            lineTo(x - step / 2, y)
                            close()
                        }
                        drawPath(path, paintColor)
                        y += step
                    }
                    x += step
                }
                // 内边框
                drawRect(
                    color = Color.White.copy(alpha = 0.35f),
                    topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                    size = Size(w - 8.dp.toPx(), h - 8.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
                )
            }
        }
    }
}

/** 筹码 */
@Composable
fun Chip(
    value: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(26.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(26.dp)) {
            drawCircle(color)
            drawCircle(
                color = color.copy(alpha = 0.6f),
                radius = size.minDimension / 2 - 2.dp.toPx(),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.5f),
                radius = 3.dp.toPx(),
            )
        }
    }
}

/** 底池筹码堆 */
@Composable
fun ChipStack(
    pot: Int,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(56.dp), contentAlignment = Alignment.Center) {
        val colors = listOf(Color(0xFFC62828), Gold, Color(0xFF1E88E5), Color(0xFF43A047))
        colors.forEachIndexed { i, c ->
            Chip(
                value = pot,
                color = c,
                modifier = Modifier.offset(
                    x = ((i % 2) * 10 - 5).dp,
                    y = (-(i / 2) * 8).dp,
                ),
            )
        }
    }
}

/** 玩家头像 */
@Composable
fun Avatar(
    name: String,
    index: Int,
    isTurn: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = if (isTurn) MaterialTheme.colorScheme.primary else avatarColor(index),
        border = if (isTurn) BorderStroke(2.dp, Gold) else null,
        modifier = modifier.size(36.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                name.takeLast(1),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }
    }
}

/** 牌桌背景：墨绿毡布 + 径向光晕 */
@Composable
fun TableBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF0B3D24), Color(0xFF14532D), Color(0xFF0B3D24)),
            ),
        )
        // 中央径向光晕
        val cx = size.width / 2f
        val cy = size.height * 0.42f
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF2E7D32).copy(alpha = 0.55f), Color.Transparent),
            ),
            radius = size.minDimension * 0.55f,
            center = Offset(cx, cy),
        )
        // 桌边描边
        drawCircle(
            color = Color(0xFF0A2F1C),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6.dp.toPx()),
            radius = size.minDimension * 0.47f,
            center = Offset(cx, cy),
        )
    }
}
