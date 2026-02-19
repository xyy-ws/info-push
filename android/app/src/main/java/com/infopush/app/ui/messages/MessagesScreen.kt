package com.infopush.app.ui.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MessagesScreen(
    viewModel: MessagesViewModel,
    onGoToSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text("消息中心")
        when {
            state.loading -> Text("加载中...")
            state.error != null -> Text(state.error ?: "加载失败")
            state.items.isEmpty() -> Text("暂无消息")
            else -> state.items.forEach { Text("• ${it.title}") }
        }
        if (state.fromMock) Text("当前显示 mock 数据")
        Button(onClick = viewModel::reload) { Text("手动同步远端") }
        Button(onClick = onGoToSettings) { Text("去设置") }
    }
}
