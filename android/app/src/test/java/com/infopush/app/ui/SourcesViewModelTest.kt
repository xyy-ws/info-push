package com.infopush.app.ui

import com.infopush.app.data.local.entity.SourceEntity
import com.infopush.app.data.repo.RefreshResult
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
    fun `api fail with fallback should expose mock state and data`() = runTest {
        val sourceFlow = MutableStateFlow(listOf(SourceEntity("mock-tech", "Mock 科技")))
        val dispatcher = StandardTestDispatcher(testScheduler)

        val viewModel = SourcesViewModel(
            observeSources = { sourceFlow },
            refreshSources = { RefreshResult.Success(fromMock = true) },
            dispatcher = dispatcher
        )

        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.loading)
        assertTrue(state.fromMock)
        assertEquals(1, state.items.size)
    }

    @Test
    fun `api fail without fallback should expose error`() = runTest {
        val sourceFlow = MutableStateFlow(emptyList<SourceEntity>())
        val dispatcher = StandardTestDispatcher(testScheduler)

        val viewModel = SourcesViewModel(
            observeSources = { sourceFlow },
            refreshSources = { RefreshResult.Error("network down") },
            dispatcher = dispatcher
        )

        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.loading)
        assertEquals("network down", state.error)
    }
}
