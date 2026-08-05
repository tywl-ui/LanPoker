package com.lanpoker.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lanpoker.app.ai.AiPrefs
import com.lanpoker.app.ui.settings.AiSettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun AiSettingsScreen(
    onBack: () -> Unit,
    viewModel: AiSettingsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = viewModel.state
    var testing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.load(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        TextButton(onClick = onBack) { Text("← 返回") }
        Text("AI 设置", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "把你自己的大模型 API 配进来，AI 对手就用它思考；没配或失败时自动用内置 AI 兜底。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))

        Row2(label = "使用大模型 API") {
            Switch(checked = state.useLlm, onCheckedChange = { viewModel.setUseLlm(it) })
        }

        OutlinedTextField(
            value = state.baseUrl,
            onValueChange = viewModel::setBaseUrl,
            label = { Text("API 地址（Base URL）") },
            placeholder = { Text("https://api.deepseek.com（OpenAI 兼容）") },
            singleLine = true,
            enabled = state.useLlm,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = state.apiKey,
            onValueChange = viewModel::setApiKey,
            label = { Text("API Key") },
            singleLine = true,
            enabled = state.useLlm,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = state.model,
            onValueChange = viewModel::setModel,
            label = { Text("模型名") },
            placeholder = { Text("deepseek-chat / qwen-plus / glm-4 / gpt-4o-mini ...") },
            singleLine = true,
            enabled = state.useLlm,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))
        Text(
            "支持任意 OpenAI 兼容接口：DeepSeek / 通义千问 / 智谱 GLM / OpenAI 等，填入自己的 Base URL + Key 即可。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                viewModel.save(context)
                Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("保存") }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                scope.launch {
                    testing = true
                    val err = viewModel.testConnection(context)
                    testing = false
                    Toast.makeText(
                        context,
                        if (err == null) "连接成功，AI 已可用" else "连接失败：$err",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
            enabled = !testing,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (testing) "测试中…" else "测试连接") }
    }
}

@Composable
private fun Row2(
    label: String,
    content: @Composable () -> Unit,
) {
    Column {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(2.dp))
        content()
        Spacer(Modifier.height(8.dp))
    }
}
