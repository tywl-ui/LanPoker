package com.lanpoker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.lanpoker.app.ai.AiEngine
import com.lanpoker.app.ai.AiPrefs
import com.lanpoker.app.ui.config.ConfigScreen
import com.lanpoker.app.ui.config.GameMode
import com.lanpoker.app.ui.rules.RulesScreen
import com.lanpoker.app.ui.settings.AiSettingsScreen
import com.lanpoker.app.ui.theme.LanPokerTheme
import com.lanpoker.app.ui.zjh.ZjhGameScreen
import com.lanpoker.app.ui.zjh.ZjhQuickScreen
import com.lanpoker.core.config.GameConfig
import com.lanpoker.core.zjh.TieRule
import com.lanpoker.core.zjh.ZjhRules

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LanPokerTheme {
                // 注意：Session 含 GameConfig（不可序列化），不能用 rememberSaveable，否则进程重建时崩溃
                var session by remember { mutableStateOf<Session?>(null) }
                var showAiSettings by rememberSaveable { mutableStateOf(false) }
                var showRules by rememberSaveable { mutableStateOf(false) }

                val current = session
                when {
                    showRules -> RulesScreen(onBack = { showRules = false })
                    showAiSettings -> AiSettingsScreen(onBack = { showAiSettings = false })
                    current == null -> ConfigScreen(
                        onStart = { config, mode, aiCount, names, rules, tieRule ->
                            session = Session(config, mode, aiCount, names, rules, tieRule)
                        },
                        onOpenAiSettings = { showAiSettings = true },
                        onOpenRules = { showRules = true },
                    )
                    else -> {
                        // 保险：AI 数量最多 = 人数 - 1
                        val aiIds = if (current.aiCount <= 0) emptySet()
                        else (1..minOf(current.aiCount, current.config.playerCount - 1)).toSet()
                        val aiEngine = AiEngine(AiPrefs.load(this))
                        when (current.mode) {
                            GameMode.FRIENDS -> ZjhGameScreen(
                                config = current.config,
                                aiIds = emptySet(),
                                aiEngine = null,
                                names = current.names,
                                rules = current.rules,
                                onExit = { session = null },
                            )
                            GameMode.VS_AI_QUICK -> ZjhQuickScreen(
                                config = current.config,
                                aiIds = aiIds,
                                aiEngine = aiEngine,
                                names = current.names,
                                rules = current.rules,
                                tieRule = current.tieRule,
                                onExit = { session = null },
                            )
                            GameMode.VS_AI_FULL -> ZjhGameScreen(
                                config = current.config,
                                aiIds = aiIds,
                                aiEngine = aiEngine,
                                names = current.names,
                                rules = current.rules,
                                onExit = { session = null },
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class Session(
    val config: GameConfig,
    val mode: GameMode,
    val aiCount: Int,
    val names: List<String>,
    val rules: ZjhRules,
    val tieRule: TieRule,
)
