package com.infopush.app.ui.favorites

import com.infopush.app.data.repo.RefreshResult
import com.infopush.app.domain.FeedItem
import com.infopush.app.ui.common.ListUiState
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

class FavoritesViewModel(
    private val observeFavorites: () -> Flow<List<FeedItem>>,
    private val refreshFavorites: suspend () -> RefreshResult,
    private val removeFavorite: suspend (String) -> Unit,
    initialQuery: String = "",
    dispatcher: CoroutineDispatcher = Dispatchers.Main
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val _uiState = MutableStateFlow(ListUiState<FeedItem>())
    val uiState: StateFlow<ListUiState<FeedItem>> = _uiState.asStateFlow()

    private val _query = MutableStateFlow(initialQuery)
    val query: StateFlow<String> = _query.asStateFlow()

    private val _events = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 4)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        scope.launch {
            observeFavorites().collectLatest { favorites ->
                _uiState.value = _uiState.value.copy(loading = false, items = favorites)
            }
        }
    }

    fun updateQuery(input: String) {
        _query.value = input
    }

    fun filteredItems(): List<FeedItem> {
        val keyword = _query.value.trim().lowercase()
        if (keyword.isBlank()) return _uiState.value.items
        return _uiState.value.items.filter {
            it.title.lowercase().contains(keyword) || it.url.lowercase().contains(keyword)
        }
    }

    fun reload() {
        scope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            when (val result = refreshFavorites()) {
                is RefreshResult.Success -> {
                    _uiState.value = _uiState.value.copy(loading = false, fromMock = result.fromMock)
                }

                is RefreshResult.Error -> {
                    _uiState.value = _uiState.value.copy(loading = false, error = result.message)
                }
            }
        }
    }

    fun deleteFavorite(itemId: String) {
        scope.launch {
            runCatching { removeFavorite(itemId) }
                .onSuccess { _events.emit("已删除收藏") }
                .onFailure { throwable ->
                    _events.emit("删除失败：${throwable.message ?: "未知错误"}")
                }
        }
    }
}
