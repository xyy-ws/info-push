package com.infopush.app.ui.sources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.infopush.app.link.LinkOpener
import com.infopush.app.ui.common.FeedbackSection
import com.infopush.app.ui.common.LoadingState

@Composable
fun SourcesScreen(
    viewModel: SourcesViewModel,
    onGoToFavorites: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val aiState by viewModel.aiSearchState.collectAsState()
    val sourceFilter by viewModel.sourceFilter.collectAsState()
    val filteredSources = remember(state.items, sourceFilter) { viewModel.filteredSources() }

    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("rss") }
    var tags by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val linkOpener = remember { LinkOpener(context, scope = scope) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))) {
                Text("信息源", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp))
            }
        }

        item {
            OutlinedTextField(
                value = aiState.keyword,
                onValueChange = viewModel::updateKeyword,
                label = { Text("AI 搜索关键词") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item { Button(onClick = viewModel::searchAiSources) { Text("AI 搜索信息源") } }
        if (aiState.loading) item { LoadingState("AI 搜索中...") }
        if (aiState.empty) item { Text("未找到候选信息源") }
        aiState.error?.let { item { Text(it) } }

        items(aiState.results, key = { it.url }) { item ->
            val added = aiState.addedUrls.contains(item.url.trim())
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${item.name} (${item.type})", style = MaterialTheme.typography.titleMedium)
                    Text(item.url, style = MaterialTheme.typography.bodySmall)
                    Text("推荐理由: ${item.reason.ifBlank { "-" }}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { linkOpener.open(item.url) }) { Text("打开链接") }
                        Button(onClick = { viewModel.addDiscoveredSource(item) }, enabled = !added) {
                            Text(if (added) "已添加" else "一键添加")
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("类型") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(value = tags, onValueChange = { tags = it }, label = { Text("标签") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            Button(onClick = {
                viewModel.createSource(SourceDraft(name = name, url = url, type = type, tags = tags))
                name = ""
                url = ""
                tags = ""
            }) { Text("新增信息源") }
        }

        item {
            OutlinedTextField(
                value = sourceFilter,
                onValueChange = viewModel::updateSourceFilter,
                label = { Text("搜索本地信息源（名称/URL/标签）") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            FeedbackSection(
                loading = state.loading,
                error = state.error,
                notice = state.notice,
                isEmpty = filteredSources.isEmpty(),
                emptyText = if (state.items.isEmpty()) "暂无信息源" else "筛选后无匹配信息源"
            )
        }

        items(filteredSources, key = { it.id }) { source ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${source.name} (${source.type})", style = MaterialTheme.typography.titleMedium)
                        Text(source.url, style = MaterialTheme.typography.bodySmall)
                        Text("tags: ${source.tags.ifBlank { "-" }}")
                        Text(if (source.enabled) "状态: 已启用" else "状态: 已禁用")
                        if (source.url.isNotBlank()) {
                            OutlinedButton(onClick = { linkOpener.open(source.url) }) { Text("打开链接") }
                        }
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.toggleSource(source) }) {
                            Text(if (source.enabled) "禁用" else "启用")
                        }
                        OutlinedButton(onClick = { viewModel.removeSource(source.id) }) { Text("删除") }
                    }
                }
            }
        }

        item { if (state.fromMock) Text("当前显示 mock 数据") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::manualSync) { Text("手动同步远端") }
                OutlinedButton(onClick = onGoToFavorites) { Text("去收藏") }
            }
        }
    }
}
