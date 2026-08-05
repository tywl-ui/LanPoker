package com.lanpoker.app.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * 顶部动作横幅：显示最近一次动作（含倍数/分数），自动滑入滑出。
 */
@Composable
fun ActionBanner(
    action: String?,
    key: Any?,
    modifier: Modifier = Modifier,
) {
    var visible by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key) {
        if (action != null) {
            visible = true
            delay(1700)
            visible = false
        }
    }
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(tween(250)) { -it } + fadeIn(tween(250)),
            exit = slideOutVertically(tween(250)) { -it } + fadeOut(tween(250)),
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xDD14532D),
                border = BorderStroke(1.dp, Gold),
                shadowElevation = 4.dp,
            ) {
                Text(
                    action ?: "",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                )
            }
        }
    }
}
