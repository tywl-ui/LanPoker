package com.lanpoker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.lanpoker.app.ui.config.ConfigScreen
import com.lanpoker.app.ui.theme.LanPokerTheme
import com.lanpoker.app.ui.zjh.ZjhGameScreen
import com.lanpoker.core.config.GameConfig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LanPokerTheme {
                var config by rememberSaveable { mutableStateOf<GameConfig?>(null) }
                val current = config
                if (current == null) {
                    ConfigScreen(
                        onStart = { config = it },
                    )
                } else {
                    ZjhGameScreen(
                        config = current,
                        onExit = { config = null },
                    )
                }
            }
        }
    }
}
