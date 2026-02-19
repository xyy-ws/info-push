package com.infopush.app.ui

import com.infopush.app.data.local.entity.SourceEntity
import com.infopush.app.data.repo.AddFavoriteResult
import com.infopush.app.data.repo.RefreshResult
import com.infopush.app.domain.FeedItem
import com.infopush.app.ui.feed.FeedViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {

    @Test
    fun `switch source should update feed items`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val sourceFlow = MutableStateFlow(
            listOf(
                SourceEntity(id = "source-a", name = "A", enabled = true),
                SourceEntity(id = "source-b", name = "B", enabled = true)
            )
        )

        val feedMap = mapOf(
            "source-a" to MutableStateFlow(
                listOf(
                    FeedItem(
                        id = "a1",
                        sourceId = "source-a",
                        title = "A-1",
                        summary = "",
                        url = "",
                        publishedAt = ""
                    )
                )
            ),
            "source-b" to MutableStateFlow(
                listOf(
                    FeedItem(
                        id = "b1",
                        sourceId = "source-b",
                        title = "B-1",
                        summary = "",
                        url = "",
                        publishedAt = ""
                    )
                )
            )
        )

        var persistedId: String? = null
        val viewModel = FeedViewModel(
            observeSources = { sourceFlow },
            observeFeed = { sourceId: String -> feedMap[sourceId] ?: flowOf(emptyList()) },
            observeFavoriteItemIds = { flowOf(emptySet()) },
            refreshSourcesAndFeed = { RefreshResult.Success() },
            refreshSource = { RefreshResult.Success() },
            addFavorite = { AddFavoriteResult.Success },
            removeFavorite = { },
            getPersistedSelectedSourceId = { persistedId },
            persistSelectedSourceId = { persistedId = it },
            dispatcher = dispatcher
        )

        advanceUntilIdle()
        assertEquals("source-a", viewModel.uiState.value.selectedSourceId)
        assertEquals(listOf("A-1"), viewModel.uiState.value.items.map { it.title })

        viewModel.selectSource("source-b")
        advanceUntilIdle()

        assertEquals("source-b", viewModel.uiState.value.selectedSourceId)
        assertEquals(listOf("B-1"), viewModel.uiState.value.items.map { it.title })
        assertEquals("source-b", persistedId)
    }

    @Test
    fun `toggle favorite should add then remove when status changes`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val sourceFlow = MutableStateFlow(listOf(SourceEntity(id = "source-a", name = "A", enabled = true)))
        val item = FeedItem("a1", "source-a", "A-1", "", "", "")
        val favoriteIdsFlow = MutableStateFlow(emptySet<String>())
        var addCount = 0
        var removeCount = 0

        val viewModel = FeedViewModel(
            observeSources = { sourceFlow },
            observeFeed = { flowOf(listOf(item)) },
            observeFavoriteItemIds = { favoriteIdsFlow },
            refreshSourcesAndFeed = { RefreshResult.Success() },
            refreshSource = { RefreshResult.Success() },
            addFavorite = {
                addCount++
                favoriteIdsFlow.value = setOf(item.id)
                AddFavoriteResult.Success
            },
            removeFavorite = {
                removeCount++
                favoriteIdsFlow.value = emptySet()
            },
            getPersistedSelectedSourceId = { "source-a" },
            persistSelectedSourceId = { },
            dispatcher = dispatcher
        )

        advanceUntilIdle()
        assertTrue(!viewModel.uiState.value.favoriteItemIds.contains(item.id))

        viewModel.toggleFavorite(item)
        advanceUntilIdle()
        assertEquals(1, addCount)
        assertTrue(viewModel.uiState.value.favoriteItemIds.contains(item.id))

        viewModel.toggleFavorite(item)
        advanceUntilIdle()
        assertEquals(1, removeCount)
        assertTrue(!viewModel.uiState.value.favoriteItemIds.contains(item.id))
    }

    @Test
    fun `should use persisted selected source when available`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val sourceFlow = MutableStateFlow(
            listOf(
                SourceEntity(id = "source-a", name = "A", enabled = true),
                SourceEntity(id = "source-b", name = "B", enabled = true)
            )
        )

        val viewModel = FeedViewModel(
            observeSources = { sourceFlow },
            observeFeed = { sourceId: String -> flowOf(listOf(FeedItem("id-$sourceId", sourceId, "title-$sourceId", "", "", ""))) },
            observeFavoriteItemIds = { flowOf(emptySet()) },
            refreshSourcesAndFeed = { RefreshResult.Success() },
            refreshSource = { RefreshResult.Success() },
            addFavorite = { AddFavoriteResult.Success },
            removeFavorite = { },
            getPersistedSelectedSourceId = { "source-b" },
            persistSelectedSourceId = {},
            dispatcher = dispatcher
        )

        advanceUntilIdle()
        assertEquals("source-b", viewModel.uiState.value.selectedSourceId)
    }
}
