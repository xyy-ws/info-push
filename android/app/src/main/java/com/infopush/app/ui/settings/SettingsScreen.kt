package com.infopush.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.infopush.app.domain.ImportMode

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onGoToMessages: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("设置")
        OutlinedTextField(
            value = state.jsonText,
            onValueChange = viewModel::updateJsonText,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("JSON 数据") }
        )
        Button(onClick = onGoToMessages) { Text("消息中心") }
        Button(onClick = viewModel::exportData) { Text("导出本地数据") }
        Button(onClick = { viewModel.importData(ImportMode.REPLACE) }) { Text("导入数据（replace）") }
        Button(onClick = { viewModel.importData(ImportMode.MERGE) }) { Text("导入数据（merge）") }
        if (state.message.isNotBlank()) Text(state.message)
    }
}
