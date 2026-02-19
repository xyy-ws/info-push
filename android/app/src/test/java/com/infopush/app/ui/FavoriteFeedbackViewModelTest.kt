package com.infopush.app.ui

import com.infopush.app.ui.favorites.FavoriteFeedbackViewModel
import com.infopush.app.ui.favorites.UiEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteFeedbackViewModelTest {

    @Test
    fun clickFavorite_emitsToast_andUpdatesButtonState() = runTest {
        val viewModel = FavoriteFeedbackViewModel()

        viewModel.onFavoriteClick()

        val event = viewModel.events.first()
        val state = viewModel.uiState.value

        assertTrue(event is UiEvent.Toast)
        assertEquals("收藏成功", (event as UiEvent.Toast).message)
        assertTrue(state.isFavorited)
        assertEquals("已收藏", state.buttonText)
    }
}
