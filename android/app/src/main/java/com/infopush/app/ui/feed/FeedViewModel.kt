package com.infopush.app.ui.feed

import com.infopush.app.data.local.entity.SourceEntity
import com.infopush.app.data.repo.RefreshResult
import com.infopush.app.domain.FeedItem
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
    val items: List<FeedItem> = emptyList(),
    val error: String? = null,
    val fromMock: Boolean = false,
    val sourceName: String = ""
)

class FeedViewModel(
    private val observeSources: () -> Flow<List<SourceEntity>>,
    private val observeFeed: (String) -> Flow<List<FeedItem>>,
    private val refreshSourcesAndFeed: suspend () -> RefreshResult,
    dispatcher: CoroutineDispatcher = Dispatchers.Main
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            observeSources().collectLatest { sources ->
                val source = sources.firstOrNull()
                if (source == null) {
                    _uiState.value = _uiState.value.copy(loading = false, items = emptyList(), sourceName = "")
                    return@collectLatest
                }

                _uiState.value = _uiState.value.copy(sourceName = source.name)
                observeFeed(source.id).collectLatest { feed ->
                    _uiState.value = _uiState.value.copy(loading = false, items = feed)
                }
            }
        }
        reload()
    }

    fun reload() {
        scope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            when (val result = refreshSourcesAndFeed()) {
                is RefreshResult.Success -> {
                    _uiState.value = _uiState.value.copy(loading = false, fromMock = result.fromMock)
                }

                is RefreshResult.Error -> {
                    _uiState.value = _uiState.value.copy(loading = false, error = result.message)
                }
            }
        }
    }
}
