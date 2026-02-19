package com.infopush.app.ui.common

data class ListUiState<T>(
    val loading: Boolean = true,
    val items: List<T> = emptyList(),
    val error: String? = null,
    val fromMock: Boolean = false
)
