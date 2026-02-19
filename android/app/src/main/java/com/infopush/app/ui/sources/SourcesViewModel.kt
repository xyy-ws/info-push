package com.infopush.app.ui.sources

import com.infopush.app.data.local.entity.SourceEntity
import com.infopush.app.data.repo.RefreshResult
import com.infopush.app.ui.common.ListUiState
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

data class SourceDraft(
    val name: String = "",
    val url: String = "",
    val type: String = "rss",
    val tags: String = ""
)

class SourcesViewModel(
    private val observeSources: () -> Flow<List<SourceEntity>>,
    private val refreshSources: suspend () -> RefreshResult,
    private val addSource: suspend (SourceDraft) -> Unit,
    private val setSourceEnabled: suspend (sourceId: String, enabled: Boolean) -> Unit,
    private val deleteSource: suspend (sourceId: String) -> Unit,
    dispatcher: CoroutineDispatcher = Dispatchers.Main
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val _uiState = MutableStateFlow(ListUiState<SourceEntity>(loading = true))
    val uiState: StateFlow<ListUiState<SourceEntity>> = _uiState.asStateFlow()

    init {
        scope.launch {
            observeSources().collectLatest { sources ->
                _uiState.value = _uiState.value.copy(loading = false, items = sources)
            }
        }
    }

    fun manualSync() {
        scope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            when (val result = refreshSources()) {
                is RefreshResult.Success -> {
                    _uiState.value = _uiState.value.copy(loading = false, fromMock = result.fromMock)
                }

                is RefreshResult.Error -> {
                    _uiState.value = _uiState.value.copy(loading = false, error = result.message)
                }
            }
        }
    }

    fun createSource(draft: SourceDraft) {
        if (draft.name.isBlank() || draft.url.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "名称和 URL 不能为空")
            return
        }
        scope.launch {
            addSource(draft)
        }
    }

    fun toggleSource(source: SourceEntity) {
        scope.launch {
            setSourceEnabled(source.id, !source.enabled)
        }
    }

    fun removeSource(sourceId: String) {
        scope.launch {
            deleteSource(sourceId)
        }
    }
}
