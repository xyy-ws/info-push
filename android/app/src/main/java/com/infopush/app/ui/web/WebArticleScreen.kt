package com.infopush.app.ui.web

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebArticleScreen(
    title: String,
    viewModel: WebArticleViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    val initialUrl by rememberUpdatedState(viewModel.openableUrl())

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(text = title, maxLines = 1) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
                }
            }
        )

        if (state.loading) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("正在解析链接...", style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (!state.loading && state.error != null) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(state.error ?: "链接不可用", color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = {
                    val target = state.fallbackUrl
                    if (target.isNotBlank()) openExternal(context, target)
                }) {
                    Text("尝试外部打开")
                }
            }
        } else if (!state.loading && initialUrl.isNotBlank()) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                            }
                        }
                        webChromeClient = WebChromeClient()
                        loadUrl(initialUrl)
                        webViewRef = this
                    }
                },
                update = { view ->
                    canGoBack = view.canGoBack()
                    canGoForward = view.canGoForward()
                    if (view.url.isNullOrBlank()) {
                        view.loadUrl(initialUrl)
                    }
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { webViewRef?.goBack() }, enabled = canGoBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "网页后退")
            }
            IconButton(onClick = { webViewRef?.goForward() }, enabled = canGoForward) {
                Icon(Icons.Outlined.ArrowForward, contentDescription = "网页前进")
            }
            IconButton(onClick = { webViewRef?.reload() }) {
                Icon(Icons.Outlined.Refresh, contentDescription = "刷新")
            }
            IconButton(onClick = {
                val target = webViewRef?.url ?: initialUrl
                if (target.isNotBlank()) openExternal(context, target)
            }) {
                Icon(Icons.Outlined.OpenInBrowser, contentDescription = "外部打开")
            }
            IconButton(onClick = { viewModel.toggleFavorite() }) {
                Icon(Icons.Outlined.Favorite, contentDescription = if (state.isFavorite) "取消收藏" else "收藏")
            }
            IconButton(onClick = {
                val target = webViewRef?.url ?: initialUrl
                if (target.isNotBlank()) shareLink(context, title, target)
            }) {
                Icon(Icons.Outlined.Share, contentDescription = "分享")
            }
            IconButton(onClick = {
                val target = webViewRef?.url ?: initialUrl
                if (target.isNotBlank()) copyLink(context, target)
            }) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = "复制链接")
            }
        }
    }
}

private fun openExternal(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }.onFailure {
        Toast.makeText(context, "无法外部打开", Toast.LENGTH_SHORT).show()
    }
}

private fun shareLink(context: Context, title: String, url: String) {
    val text = if (title.isBlank()) url else "$title\n$url"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "分享链接"))
}

private fun copyLink(context: Context, url: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("article_url", url))
    Toast.makeText(context, "链接已复制", Toast.LENGTH_SHORT).show()
}
