package com.infopush.app.ui.web

import com.infopush.app.data.repo.AddFavoriteResult
import com.infopush.app.domain.FeedItem
import com.infopush.app.link.PreparedLink
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

data class WebArticleUiState(
    val loading: Boolean = true,
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

    init {
        scope.launch {
            observeFavoriteItemIds().collectLatest { ids ->
                _uiState.value = _uiState.value.copy(isFavorite = ids.contains(articleItem.id))
            }
        }
        scope.launch {
            when (val result = prepareLink(articleItem.url)) {
                is PreparedLink.Invalid -> {
                    _uiState.value = _uiState.value.copy(loading = false, error = result.message)
                }
                is PreparedLink.Valid -> {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        resolvedUrl = result.finalUrl,
                        fallbackUrl = result.fallbackUrl,
                        error = null
                    )
                }
            }
        }
    }

    fun toggleFavorite() {
        scope.launch {
            if (_uiState.value.isFavorite) {
                removeFavorite(articleItem.id)
            } else {
                addFavorite(articleItem)
            }
        }
    }

    fun openableUrl(): String {
        val state = _uiState.value
        return state.resolvedUrl.ifBlank { state.fallbackUrl }
    }
}
