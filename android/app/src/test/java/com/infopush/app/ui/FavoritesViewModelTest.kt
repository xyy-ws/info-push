package com.infopush.app.ui

import com.infopush.app.data.repo.RefreshResult
import com.infopush.app.domain.FeedItem
import com.infopush.app.ui.favorites.FavoritesViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {

    @Test
    fun `delete favorite should update list immediately and emit feedback`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val item = FeedItem(
            id = "fav-1",
            sourceId = "source-a",
            title = "Title",
            summary = "",
            url = "https://example.com",
            publishedAt = ""
        )
        val favoritesFlow = MutableStateFlow(listOf(item))

        val viewModel = FavoritesViewModel(
            observeFavorites = { favoritesFlow },
            refreshFavorites = { RefreshResult.Success() },
            removeFavorite = { id ->
                favoritesFlow.value = favoritesFlow.value.filterNot { it.id == id }
            },
            dispatcher = dispatcher
        )

        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.items.size)

        val feedback = async { viewModel.events.first() }
        viewModel.deleteFavorite(item.id)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.items.isEmpty())
        assertEquals("已删除收藏", feedback.await())
    }
}
