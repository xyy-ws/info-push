package com.infopush.app.ui.web

import com.infopush.app.data.repo.AddFavoriteResult
import com.infopush.app.domain.FeedItem
import com.infopush.app.link.LinkNormalizer
import com.infopush.app.link.PreparedLink
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class WebArticleUiState(
    val loading: Boolean = true,
    val resolving: Boolean = false,
    val resolvedUrl: String = "",
    val fallbackUrl: String = "",
    val isFavorite: Boolean = false,
    val error: String? = null
)

class WebArticleViewModel(
    private val articleItem: FeedItem,
    private val prepareLink: suspend (String) -> PreparedLink,
    private val observeFavoriteItemIds: () -> Flow<Set<String>>,
    private val addFavorite: suspend (FeedItem) -> AddFavoriteResult,
    private val removeFavorite: suspend (String) -> Unit,
    dispatcher: CoroutineDispatcher = Dispatchers.Main
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val _uiState = MutableStateFlow(WebArticleUiState())
    val uiState: StateFlow<WebArticleUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 4)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        val normalizedFallback = LinkNormalizer.normalize(articleItem.url)
        if (normalizedFallback == null) {
            _uiState.value = _uiState.value.copy(
                loading = false,
                resolving = false,
                error = "链接无效：${articleItem.url}",
                fallbackUrl = ""
            )
        } else {
            _uiState.value = _uiState.value.copy(
                loading = false,
                resolving = true,
                fallbackUrl = normalizedFallback,
                resolvedUrl = normalizedFallback,
                error = null
            )
        }

        scope.launch {
            observeFavoriteItemIds().collectLatest { ids ->
                _uiState.value = _uiState.value.copy(isFavorite = ids.contains(articleItem.id))
            }
        }

        if (normalizedFallback != null) {
            scope.launch {
                when (val result = prepareLink(normalizedFallback)) {
                    is PreparedLink.Invalid -> {
                        _uiState.value = _uiState.value.copy(resolving = false, error = result.message)
                    }
                    is PreparedLink.Valid -> {
                        _uiState.value = _uiState.value.copy(
                            resolving = false,
                            resolvedUrl = result.finalUrl.ifBlank { result.fallbackUrl },
                            fallbackUrl = result.fallbackUrl,
                            error = null
                        )
                    }
                }
            }
        }
    }

    fun toggleFavorite() {
        scope.launch {
            val message = runCatching {
                if (_uiState.value.isFavorite) {
                    removeFavorite(articleItem.id)
                    "已取消收藏"
                } else {
                    when (addFavorite(articleItem)) {
                        AddFavoriteResult.Success -> "收藏成功"
                        AddFavoriteResult.Duplicated -> "已在收藏列表"
                    }
                }
            }.getOrElse { throwable ->
                "操作失败：${throwable.message ?: "未知错误"}"
            }
            _events.emit(message)
        }
    }

    fun openableUrl(): String {
        val state = _uiState.value
        return state.resolvedUrl.ifBlank { state.fallbackUrl }
    }
}
