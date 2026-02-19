package com.infopush.app.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoadingState(text: String = "加载中...") {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
fun EmptyState(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun ErrorState(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error
    )
}

@Composable
fun NoticeState(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun FeedbackSection(
    loading: Boolean,
    error: String?,
    notice: String?,
    isEmpty: Boolean,
    emptyText: String,
    loadingText: String = "加载中..."
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        when {
            loading -> LoadingState(loadingText)
            error != null -> ErrorState(error)
            notice != null -> NoticeState(notice)
            isEmpty -> EmptyState(emptyText)
        }
    }
}
