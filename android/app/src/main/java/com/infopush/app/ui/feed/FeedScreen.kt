package com.infopush.app.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.infopush.app.link.LinkOpener
import com.infopush.app.ui.common.FeedbackSection

@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onGoToSources: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val linkOpener = remember { LinkOpener(context, scope = scope) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text("首页")
        if (state.sources.isNotEmpty()) {
            Text("信息源选择")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(state.sources, key = { it.id }) { source ->
                    val selected = source.id == state.selectedSourceId
                    if (selected) {
                        Button(onClick = { viewModel.selectSource(source.id) }) {
                            Text(source.name)
                        }
                    } else {
                        OutlinedButton(onClick = { viewModel.selectSource(source.id) }) {
                            Text(source.name)
                        }
                    }
                }
            }
        }
        if (state.sourceName.isNotBlank()) Text("当前源：${state.sourceName}")

        FeedbackSection(
            loading = state.loading,
            error = state.error,
            isEmpty = state.selectedSourceId == null || state.items.isEmpty(),
            emptyText = if (state.selectedSourceId == null) "暂无可用信息源，请先添加或启用信息源" else "该信息源暂无内容"
        )

        if (!state.loading && state.error == null && state.selectedSourceId != null && state.items.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.items.take(20), key = { it.id }) { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("• ${item.title}")
                        if (item.url.isNotBlank()) {
                            OutlinedButton(onClick = { linkOpener.open(item.url) }) { Text("打开原文") }
                        }
                    }
                }
            }
        }

        if (state.fromMock) Text("当前显示 mock 数据")
        Button(onClick = viewModel::refreshCurrentSource) { Text("刷新当前信息源") }
        Button(onClick = viewModel::reload) { Text("同步远端首页") }
        Button(onClick = onGoToSources) { Text("查看信息源") }
    }
}
