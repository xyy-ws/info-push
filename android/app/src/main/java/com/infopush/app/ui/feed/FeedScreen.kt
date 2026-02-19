package com.infopush.app.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.infopush.app.domain.FeedItem
import com.infopush.app.ui.common.readableTime

@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onGoToSources: () -> Unit,
    onOpenArticle: (FeedItem) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FeedHeader(
            sourceName = state.sourceName,
            onRefresh = viewModel::refreshCurrentSource,
            onSync = viewModel::reload
        )

        if (state.sources.isNotEmpty()) {
            SourceSwitcher(
                sources = state.sources,
                selectedSourceId = state.selectedSourceId,
                onSelect = viewModel::selectSource
            )
        }

        when {
            state.loading -> {
                GuidedStateCard(
                    title = "正在刷新内容",
                    body = "稍等片刻，我们会优先展示本地缓存并同步最新内容。",
                    primaryText = "同步远端首页",
                    onPrimaryClick = viewModel::reload,
                    secondaryText = "管理信息源",
                    onSecondaryClick = onGoToSources
                )
            }

            state.error != null -> {
                GuidedStateCard(
                    title = "加载失败",
                    body = state.error ?: "发生未知错误",
                    primaryText = "重试刷新",
                    onPrimaryClick = viewModel::refreshCurrentSource,
                    secondaryText = "同步远端首页",
                    onSecondaryClick = viewModel::reload
                )
            }

            state.selectedSourceId == null -> {
                GuidedStateCard(
                    title = "暂无可用信息源",
                    body = "先添加或启用信息源，然后返回首页即可看到信息流。",
                    primaryText = "查看信息源",
                    onPrimaryClick = onGoToSources,
                    secondaryText = "同步远端首页",
                    onSecondaryClick = viewModel::reload
                )
            }

            state.items.isEmpty() -> {
                GuidedStateCard(
                    title = "当前源暂无内容",
                    body = "可尝试刷新当前信息源，或切换到其他信息源查看。",
                    primaryText = "刷新当前信息源",
                    onPrimaryClick = viewModel::refreshCurrentSource,
                    secondaryText = "查看信息源",
                    onSecondaryClick = onGoToSources
                )
            }

            else -> {
                if (state.fromMock) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = "当前显示 mock 数据",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.items.take(20), key = { it.id }) { item ->
                        FeedItemCard(
                            item = item,
                            isFavorite = state.favoriteItemIds.contains(item.id),
                            onOpen = { onOpenArticle(item) },
                            onToggleFavorite = { viewModel.toggleFavorite(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedHeader(
    sourceName: String,
    onRefresh: () -> Unit,
    onSync: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text("首页", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (sourceName.isNotBlank()) "当前源：$sourceName" else "请选择信息源",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "刷新当前信息源")
                }
                IconButton(onClick = onSync) {
                    Icon(Icons.Outlined.CloudSync, contentDescription = "同步远端首页")
                }
            }
        }
    }
}

@Composable
private fun SourceSwitcher(
    sources: List<com.infopush.app.data.local.entity.SourceEntity>,
    selectedSourceId: String?,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "信息源",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            items(sources, key = { it.id }) { source ->
                FilterChip(
                    selected = source.id == selectedSourceId,
                    onClick = { onSelect(source.id) },
                    label = { Text(source.name) },
                    leadingIcon = {
                        if (source.id == selectedSourceId) {
                            Icon(
                                imageVector = Icons.Outlined.RssFeed,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun FeedItemCard(item: FeedItem, isFavorite: Boolean, onOpen: () -> Unit, onToggleFavorite: () -> Unit) {
    ElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (item.summary.isNotBlank()) {
                Text(
                    text = item.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = readableTime(item.publishedAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onToggleFavorite) {
                        Text(if (isFavorite) "已收藏" else "收藏")
                    }
                    if (item.url.isNotBlank()) {
                        OutlinedButton(onClick = onOpen) {
                            Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Box(modifier = Modifier.width(6.dp))
                            Text("打开原文")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuidedStateCard(
    title: String,
    body: String,
    primaryText: String,
    onPrimaryClick: () -> Unit,
    secondaryText: String,
    onSecondaryClick: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPrimaryClick) { Text(primaryText) }
                OutlinedButton(onClick = onSecondaryClick) { Text(secondaryText) }
            }
        }
    }
}
