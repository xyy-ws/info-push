package com.infopush.app.ui

import com.infopush.app.data.local.entity.SourceEntity
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

        val viewModel = FeedViewModel(
            observeSources = { sourceFlow },
            observeFeed = { sourceId: String -> feedMap[sourceId] ?: flowOf(emptyList()) },
            refreshSourcesAndFeed = { RefreshResult.Success() },
            refreshSource = { RefreshResult.Success() },
            dispatcher = dispatcher
        )

        advanceUntilIdle()
        assertEquals("source-a", viewModel.uiState.value.selectedSourceId)
        assertEquals(listOf("A-1"), viewModel.uiState.value.items.map { it.title })

        viewModel.selectSource("source-b")
        advanceUntilIdle()

        assertEquals("source-b", viewModel.uiState.value.selectedSourceId)
        assertEquals(listOf("B-1"), viewModel.uiState.value.items.map { it.title })
    }
}
