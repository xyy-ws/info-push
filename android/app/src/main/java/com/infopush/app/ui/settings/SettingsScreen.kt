package com.infopush.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.infopush.app.domain.ImportMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onGoToMessages: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var exportContent by remember { mutableStateOf<String?>(null) }
    var pendingImportMode by remember { mutableStateOf(ImportMode.MERGE) }

    val createFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val payload = exportContent
        if (uri == null || payload == null) {
            viewModel.updateMessage("导出已取消")
            return@rememberLauncherForActivityResult
        }
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use {
                it.write(payload.toByteArray())
            } ?: error("无法写入目标文件")
        }
            .onSuccess { viewModel.updateMessage("导出成功：已保存到系统文件") }
            .onFailure { viewModel.updateMessage("导出失败: ${it.message}") }
    }

    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            viewModel.updateMessage("导入已取消")
            return@rememberLauncherForActivityResult
        }
        runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("无法读取所选文件")
        }
            .onSuccess { viewModel.importFromJson(it, pendingImportMode) }
            .onFailure { viewModel.updateMessage("导入失败: ${it.message}") }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
        ) {
            Text(
                "设置",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }

        OutlinedTextField(
            value = state.jsonText,
            onValueChange = viewModel::updateJsonText,
            modifier = Modifier.fillMaxWidth(),
            minLines = 8,
            label = { Text("JSON 数据") }
        )

        Button(onClick = onGoToMessages) { Text("消息中心") }

        Button(onClick = {
            viewModel.prepareExport { json ->
                exportContent = json
                val fileName = "info-push-export-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))}.json"
                createFileLauncher.launch(fileName)
            }
        }) { Text("导出 JSON 到文件") }

        RowButtons(
            primary = {
                pendingImportMode = ImportMode.REPLACE
                openFileLauncher.launch(arrayOf("application/json", "text/plain"))
            },
            primaryText = "文件导入（replace）",
            secondary = {
                pendingImportMode = ImportMode.MERGE
                openFileLauncher.launch(arrayOf("application/json", "text/plain"))
            },
            secondaryText = "文件导入（merge）"
        )

        RowButtons(
            primary = { viewModel.importData(ImportMode.REPLACE) },
            primaryText = "文本导入（replace）",
            secondary = { viewModel.importData(ImportMode.MERGE) },
            secondaryText = "文本导入（merge）"
        )

        if (state.message.isNotBlank()) Text(state.message)
    }
}

@Composable
private fun RowButtons(
    primary: () -> Unit,
    primaryText: String,
    secondary: () -> Unit,
    secondaryText: String
) {
    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = primary) { Text(primaryText) }
        OutlinedButton(onClick = secondary) { Text(secondaryText) }
    }
}
