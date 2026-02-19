package com.infopush.app.ui.messages

import com.infopush.app.data.repo.RefreshResult
import com.infopush.app.domain.InfoMessage
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

class MessagesViewModel(
    private val observeMessages: () -> Flow<List<InfoMessage>>,
    private val refreshMessages: suspend () -> RefreshResult,
    dispatcher: CoroutineDispatcher = Dispatchers.Main
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val _uiState = MutableStateFlow(ListUiState<InfoMessage>())
    val uiState: StateFlow<ListUiState<InfoMessage>> = _uiState.asStateFlow()

    init {
        scope.launch {
            observeMessages().collectLatest { messages ->
                _uiState.value = _uiState.value.copy(loading = false, items = messages)
            }
        }
    }

    fun reload() {
        scope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            when (val result = refreshMessages()) {
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
