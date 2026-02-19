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

class SourcesViewModel(
    private val observeSources: () -> Flow<List<SourceEntity>>,
    private val refreshSources: suspend () -> RefreshResult,
    dispatcher: CoroutineDispatcher = Dispatchers.Main
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val _uiState = MutableStateFlow(ListUiState<SourceEntity>())
    val uiState: StateFlow<ListUiState<SourceEntity>> = _uiState.asStateFlow()

    init {
        scope.launch {
            observeSources().collectLatest { sources ->
                _uiState.value = _uiState.value.copy(loading = false, items = sources)
            }
        }
        reload()
    }

    fun reload() {
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
}
