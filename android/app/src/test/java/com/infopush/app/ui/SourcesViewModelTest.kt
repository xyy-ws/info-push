package com.infopush.app.ui

import com.infopush.app.data.local.entity.SourceEntity
import com.infopush.app.data.repo.AddSourceResult
import com.infopush.app.data.repo.AiDiscoveredSource
import com.infopush.app.data.repo.RefreshResult
import com.infopush.app.ui.sources.SourceDraft
import com.infopush.app.ui.sources.SourcesViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SourcesViewModelTest {
    @Test
    fun `create source should call addSource`() = runTest {
        val sourceFlow = MutableStateFlow(emptyList<SourceEntity>())
        val dispatcher = StandardTestDispatcher(testScheduler)
        var added: SourceDraft? = null

        val viewModel = SourcesViewModel(
            observeSources = { sourceFlow },
            refreshSources = { RefreshResult.Success() },
            addSource = { added = it },
            setSourceEnabled = { _, _ -> },
            deleteSource = {},
            discoverSources = { emptyList() },
            addAiSourceToLocal = { AddSourceResult.Success },
            dispatcher = dispatcher
        )

        viewModel.createSource(SourceDraft(name = "Tech", url = "https://a.com", type = "rss", tags = "kotlin"))
        advanceUntilIdle()

        assertEquals("Tech", added?.name)
        assertEquals("https://a.com", added?.url)
    }

    @Test
    fun `manual sync error should expose error`() = runTest {
        val sourceFlow = MutableStateFlow(emptyList<SourceEntity>())
        val dispatcher = StandardTestDispatcher(testScheduler)

        val viewModel = SourcesViewModel(
            observeSources = { sourceFlow },
            refreshSources = { RefreshResult.Error("network down") },
            addSource = {},
            setSourceEnabled = { _, _ -> },
            deleteSource = {},
            discoverSources = { emptyList() },
            addAiSourceToLocal = { AddSourceResult.Success },
            dispatcher = dispatcher
        )

        viewModel.manualSync()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.loading)
        assertEquals("network down", state.error)
    }

    @Test
    fun `toggle source should invert enabled flag`() = runTest {
        val sourceFlow = MutableStateFlow(listOf(SourceEntity("1", "A", enabled = true)))
        val dispatcher = StandardTestDispatcher(testScheduler)
        var toggledEnabled: Boolean? = null

        val viewModel = SourcesViewModel(
            observeSources = { sourceFlow },
            refreshSources = { RefreshResult.Success(fromMock = true) },
            addSource = {},
            setSourceEnabled = { _, enabled -> toggledEnabled = enabled },
            deleteSource = {},
            discoverSources = { emptyList() },
            addAiSourceToLocal = { AddSourceResult.Success },
            dispatcher = dispatcher
        )

        viewModel.toggleSource(sourceFlow.value.first())
        advanceUntilIdle()

        assertTrue(toggledEnabled == false)
    }

    @Test
    fun `search ai sources success should update results`() = runTest {
        val sourceFlow = MutableStateFlow(emptyList<SourceEntity>())
        val dispatcher = StandardTestDispatcher(testScheduler)

        val viewModel = SourcesViewModel(
            observeSources = { sourceFlow },
            refreshSources = { RefreshResult.Success(fromMock = false) },
            addSource = {},
            setSourceEnabled = { _, _ -> },
            deleteSource = {},
            discoverSources = {
                listOf(AiDiscoveredSource(name = "Tech Daily", url = "https://x.com/rss", type = "rss", reason = "matches keyword"))
            },
            addAiSourceToLocal = { AddSourceResult.Success },
            dispatcher = dispatcher
        )

        viewModel.updateKeyword("tech")
        viewModel.searchAiSources()
        advanceUntilIdle()

        assertEquals(1, viewModel.aiSearchState.value.results.size)
        assertEquals("Tech Daily", viewModel.aiSearchState.value.results.first().name)
        assertFalse(viewModel.aiSearchState.value.loading)
    }
}
