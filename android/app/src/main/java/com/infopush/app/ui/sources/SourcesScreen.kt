package com.infopush.app.ui.sources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SourcesScreen(
    viewModel: SourcesViewModel,
    onGoToFavorites: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("rss") }
    var tags by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text("信息源")

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("类型") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = tags, onValueChange = { tags = it }, label = { Text("标签") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            viewModel.createSource(SourceDraft(name = name, url = url, type = type, tags = tags))
            name = ""
            url = ""
            tags = ""
        }) { Text("新增信息源") }

        if (state.error != null) Text(state.error ?: "")
        if (state.loading) Text("加载中...")
        if (state.items.isEmpty()) Text("暂无信息源")

        state.items.forEach { source ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${source.name} (${source.type})")
                    Text(source.url)
                    Text("tags: ${source.tags.ifBlank { "-" }}")
                    Text(if (source.enabled) "状态: 已启用" else "状态: 已禁用")
                }
                Column(horizontalAlignment = Alignment.End) {
                    Button(onClick = { viewModel.toggleSource(source) }) {
                        Text(if (source.enabled) "禁用" else "启用")
                    }
                    Button(onClick = { viewModel.removeSource(source.id) }) { Text("删除") }
                }
            }
        }

        if (state.fromMock) Text("当前显示 mock 数据")
        Button(onClick = viewModel::manualSync) { Text("手动同步远端") }
        Button(onClick = onGoToFavorites) { Text("去收藏") }
    }
}
