package com.lanpoker.app.ui.rules

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lanpoker.app.BuildConfig

const val REPO_URL = "https://github.com/tywl-ui/LanPoker"

@Composable
fun RulesScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        TextButton(onClick = onBack) { Text("← 返回") }
        Text("玩法规则", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "局域网棋牌 · 版本 ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        Section("牌型大小（从大到小）") {
            RuleRow("豹子", "三张同点数，如 AAA")
            RuleRow("顺金", "同花色连续三张，如 ♥Q♥K♥A")
            RuleRow("金花", "同花色非连续，如 ♠A♠K♠9")
            RuleRow("顺子", "点数连续（异花），A23 最小、QKA 最大；KA2 不算")
            RuleRow("对子", "一对 + 一张散牌")
            RuleRow("单张", "散牌比最大张")
            RuleRow("235", "杂色 2/3/5 吃豹子，其余情况按单张（最小）")
        }

        Section("特殊规则") {
            RuleRow("杂色 235", "只吃豹子；同花 235 算金花，不算 235")
            RuleRow("金花 235", "吃同花豹子（三张同花色同点数）；对杂花豹按正常顺序")
            RuleRow("同花豹", "点数相同时 同花豹 > 杂花豹（3 副牌玩法才有同花豹）")
            RuleRow("A 作低/高", "A23 是最小顺子，QKA 是最大顺子")
        }

        Section("王（百搭）") {
            RuleRow("自动补强", "王可代替任意牌，自动按最大牌型判定，如 王+对A = 豹子A")
            RuleRow("比牌定型", "有王的人比牌先自选一个牌型；对方有王可在能赢过的牌型里再选，选不出就输")
            RuleRow("同花豹限制", "1-2 副牌时王不能变成同花豹；3 副牌时可以")
        }

        Section("下注规则") {
            RuleRow("底注", "开局每人自动下 1 底")
            RuleRow("闷牌 / 看牌", "闷牌跟注 = level 底；看牌跟注 = 2×level 底（看牌是闷牌的 2 倍）")
            RuleRow("跟注", "必须跟上当前倍数，跟够了可以「过」")
            RuleRow("加注", "倍数必须高于上一家，可自填（上限 10 倍）；看牌加注按 2 倍计")
        }

        Section("比牌规则") {
            RuleRow("比牌费", "双方各付比牌费：闷牌 1×level，看牌 2×level")
            RuleRow("比牌限制", "三家以上只能和已看牌的玩家比；仅剩两家时可与闷牌者开牌")
            RuleRow("平局", "比牌平局时发起者输")
        }

        Section("结算与记账") {
            RuleRow("赢家", "赢家收走底池，净收 = 底池 − 自己投入")
            RuleRow("输家", "输家扣除各自投入，账目零和")
            RuleRow("平局处理", "和局重发 / 比花色 / 平局退钱（配置页可选）")
            RuleRow("账单", "结算后可在「账单」里复制全部账目文本")
        }

        Section("快局（人机连开）") {
            RuleRow("固定倍数", "每人选闷（1 底）或看（2 底），选完自动亮牌比大小")
            RuleRow("连开", "点「下一局」连续玩多把，总分自动累计")
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL))
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("查看源码仓库（GitHub）")
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "开源地址：github.com/tywl-ui/LanPoker\n遇到问题欢迎在仓库提 Issue",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(12.dp))
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            content()
        }
    }
}

@Composable
private fun RuleRow(title: String, desc: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.primary,
        ) {
            Text(
                title,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(desc, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
    }
}
