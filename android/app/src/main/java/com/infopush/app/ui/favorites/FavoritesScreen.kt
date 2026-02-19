package com.infopush.app.ui.favorites

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.infopush.app.domain.FeedItem
import com.infopush.app.ui.common.FeedbackSection
import com.infopush.app.ui.common.readableTime

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onOpenArticle: (FeedItem) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val query by viewModel.query.collectAsState()
    val filtered = remember(state.items, query) { viewModel.filteredItems() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
        ) {
            Text(
                "收藏",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::updateQuery,
                modifier = Modifier.weight(1f),
                label = { Text("搜索收藏（标题 / URL）") }
            )
            IconButton(onClick = { viewModel.updateQuery(query) }) {
                Icon(Icons.Outlined.Search, contentDescription = "搜索")
            }
            IconButton(onClick = { viewModel.updateQuery("") }) {
                Icon(Icons.Outlined.Close, contentDescription = "清空")
            }
        }

        FeedbackSection(
            loading = state.loading,
            error = state.error?.ifBlank { "加载失败，请稍后重试" },
            notice = null,
            isEmpty = filtered.isEmpty(),
            emptyText = if (state.items.isEmpty()) "暂无收藏内容" else "暂无匹配的收藏",
            loadingText = "正在加载收藏..."
        )

        if (!state.loading && state.error == null && filtered.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { item ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            Text(readableTime(item.publishedAt), style = MaterialTheme.typography.labelMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (item.url.isNotBlank()) {
                                    OutlinedButton(onClick = { onOpenArticle(item) }) {
                                        Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                        androidx.compose.foundation.layout.Box(modifier = Modifier.width(6.dp))
                                        Text("打开原文")
                                    }
                                }
                                OutlinedButton(onClick = { viewModel.deleteFavorite(item.id) }) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                    androidx.compose.foundation.layout.Box(modifier = Modifier.width(6.dp))
                                    Text("删除收藏")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (state.fromMock) {
            Text("当前显示 mock 数据", style = MaterialTheme.typography.bodySmall)
        }

    }
}
