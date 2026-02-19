package com.infopush.app.ui.feed

import com.infopush.app.data.local.entity.SourceEntity
import com.infopush.app.data.repo.AddFavoriteResult
import com.infopush.app.data.repo.RefreshResult
import com.infopush.app.domain.FeedItem
import com.infopush.app.ui.common.UserFacingError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class FeedUiState(
    val loading: Boolean = true,
    val sources: List<SourceEntity> = emptyList(),
    val selectedSourceId: String? = null,
    val items: List<FeedItem> = emptyList(),
    val favoriteItemIds: Set<String> = emptySet(),
    val error: String? = null,
    val fromMock: Boolean = false,
    val sourceName: String = ""
)

class FeedViewModel(
    private val observeSources: () -> Flow<List<SourceEntity>>,
    private val observeFeed: (String) -> Flow<List<FeedItem>>,
    private val observeFavoriteItemIds: () -> Flow<Set<String>>,
    private val refreshSourcesAndFeed: suspend () -> RefreshResult,
    private val refreshSource: suspend (String) -> RefreshResult,
    private val addFavorite: suspend (FeedItem) -> AddFavoriteResult,
    private val removeFavorite: suspend (String) -> Unit,
    private val getPersistedSelectedSourceId: suspend () -> String?,
    private val persistSelectedSourceId: suspend (String) -> Unit,
    dispatcher: CoroutineDispatcher = Dispatchers.Main
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()
    private val selectedSourceIdFlow = MutableStateFlow<String?>(null)
    private val persistedSelectedSourceIdFlow = MutableStateFlow<String?>(null)

    init {
        scope.launch {
            persistedSelectedSourceIdFlow.value = getPersistedSelectedSourceId()
        }

        scope.launch {
            observeFavoriteItemIds().collectLatest { favoriteIds ->
                _uiState.value = _uiState.value.copy(favoriteItemIds = favoriteIds)
            }
        }

        scope.launch {
            observeSources().collectLatest { allSources ->
                val sources = allSources.filter { it.enabled }
                val current = _uiState.value
                val selectedId = current.selectedSourceId?.takeIf { id -> sources.any { it.id == id } }
                    ?: persistedSelectedSourceIdFlow.value?.takeIf { id -> sources.any { it.id == id } }
                    ?: sources.firstOrNull()?.id
                val selectedSource = sources.firstOrNull { it.id == selectedId }

                _uiState.value = current.copy(
                    loading = false,
                    sources = sources,
                    selectedSourceId = selectedId,
                    sourceName = selectedSource?.name.orEmpty(),
                    items = if (selectedId == null) emptyList() else current.items
                )
                selectedSourceIdFlow.value = selectedId
                selectedId?.let { persistSelection(it) }
            }
        }

        scope.launch {
            selectedSourceIdFlow.collectLatest { sourceId ->
                if (sourceId == null) {
                    _uiState.value = _uiState.value.copy(loading = false, items = emptyList())
                    return@collectLatest
                }
                observeFeed(sourceId).collectLatest { feed ->
                    _uiState.value = _uiState.value.copy(loading = false, items = feed)
                }
            }
        }
    }

    fun selectSource(sourceId: String) {
        if (_uiState.value.selectedSourceId == sourceId) return
        val source = _uiState.value.sources.firstOrNull { it.id == sourceId } ?: return
        _uiState.value = _uiState.value.copy(selectedSourceId = sourceId, sourceName = source.name, items = emptyList())
        selectedSourceIdFlow.value = sourceId
        persistSelection(sourceId)
    }

    fun toggleFavorite(item: FeedItem) {
        scope.launch {
            if (_uiState.value.favoriteItemIds.contains(item.id)) {
                removeFavorite(item.id)
            } else {
                addFavorite(item)
            }
        }
    }

    fun reload() {
        scope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            when (val result = refreshSourcesAndFeed()) {
                is RefreshResult.Success -> {
                    _uiState.value = _uiState.value.copy(loading = false, fromMock = result.fromMock)
                }

                is RefreshResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = UserFacingError.format(result.message, fallback = "首页同步失败")
                    )
                }
            }
        }
    }

    fun refreshCurrentSource() {
        val sourceId = _uiState.value.selectedSourceId
        if (sourceId == null) {
            _uiState.value = _uiState.value.copy(error = "请先选择信息源")
            return
        }
        scope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            when (val result = refreshSource(sourceId)) {
                is RefreshResult.Success -> _uiState.value = _uiState.value.copy(loading = false)
                is RefreshResult.Error -> _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = UserFacingError.format(result.message, fallback = "信息源刷新失败")
                )
            }
        }
    }

    private fun persistSelection(sourceId: String) {
        scope.launch {
            persistSelectedSourceId(sourceId)
            persistedSelectedSourceIdFlow.value = sourceId
        }
    }
}
