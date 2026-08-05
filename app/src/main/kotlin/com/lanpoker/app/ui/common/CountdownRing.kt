package com.lanpoker.app.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 斗地主式圆形倒计时环：外圈进度随剩余时间减少，最后几秒变红。
 */
@Composable
fun CountdownRing(
    secondsLeft: Int,
    total: Int,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
) {
    val progress = (secondsLeft.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f)
    val color = when {
        secondsLeft <= 5 -> Color(0xFFFF5252)
        secondsLeft <= 10 -> Color(0xFFFFB300)
        else -> Color(0xFFFFD54F)
    }
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(size),
            color = color,
            trackColor = Color.White.copy(alpha = 0.15f),
            strokeWidth = 5.dp,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$secondsLeft",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
            Text(
                label,
                color = color,
                fontSize = 10.sp,
            )
        }
    }
}
