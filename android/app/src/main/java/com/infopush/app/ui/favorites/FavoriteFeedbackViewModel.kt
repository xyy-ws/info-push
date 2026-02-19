package com.infopush.app.ui.favorites

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface UiEvent {
    data class Toast(val message: String) : UiEvent
}

data class FavoriteFeedbackUiState(
    val isFavorited: Boolean = false,
    val buttonText: String = "收藏"
)

class FavoriteFeedbackViewModel {
    private val _uiState = MutableStateFlow(FavoriteFeedbackUiState())
    val uiState: StateFlow<FavoriteFeedbackUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(replay = 1, extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun onFavoriteClick() {
        if (_uiState.value.isFavorited) return
        _uiState.value = FavoriteFeedbackUiState(
            isFavorited = true,
            buttonText = "已收藏"
        )
        _events.tryEmit(UiEvent.Toast("收藏成功"))
    }
}
