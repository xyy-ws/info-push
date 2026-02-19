package com.infopush.app.ui

import com.infopush.app.data.repo.AddFavoriteResult
import com.infopush.app.domain.FeedItem
import com.infopush.app.link.PreparedLink
import com.infopush.app.ui.web.WebArticleViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WebArticleViewModelTest {

    @Test
    fun `prepare link should use fallback openable url when resolver fails`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val favorites = MutableStateFlow(emptySet<String>())
        val item = FeedItem(
            id = "a1",
            sourceId = "source-a",
            title = "Title",
            summary = "",
            url = "https://fallback.example.com",
            publishedAt = ""
        )

        val viewModel = WebArticleViewModel(
            articleItem = item,
            prepareLink = {
                PreparedLink.Valid(
                    finalUrl = "",
                    fallbackUrl = "https://fallback.example.com"
                )
            },
            observeFavoriteItemIds = { favorites },
            addFavorite = { AddFavoriteResult.Success },
            removeFavorite = { },
            dispatcher = dispatcher
        )

        advanceUntilIdle()
        assertEquals("https://fallback.example.com", viewModel.openableUrl())
        assertTrue(!viewModel.uiState.value.isFavorite)

        favorites.value = setOf("a1")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isFavorite)
    }

    @Test
    fun `toggle favorite should emit feedback and update state from datasource`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val favorites = MutableStateFlow(emptySet<String>())
        val item = FeedItem(
            id = "fav-1",
            sourceId = "source-a",
            title = "Title",
            summary = "",
            url = "https://fallback.example.com",
            publishedAt = ""
        )

        val viewModel = WebArticleViewModel(
            articleItem = item,
            prepareLink = { PreparedLink.Valid("https://fallback.example.com", "https://fallback.example.com") },
            observeFavoriteItemIds = { favorites },
            addFavorite = {
                favorites.value = favorites.value + item.id
                AddFavoriteResult.Success
            },
            removeFavorite = { id ->
                favorites.value = favorites.value - id
            },
            dispatcher = dispatcher
        )

        advanceUntilIdle()
        viewModel.toggleFavorite()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isFavorite)
        assertEquals("收藏成功", viewModel.events.first())

        viewModel.toggleFavorite()
        advanceUntilIdle()
        assertTrue(!viewModel.uiState.value.isFavorite)
    }
}
