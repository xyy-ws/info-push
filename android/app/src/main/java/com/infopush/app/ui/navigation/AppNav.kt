package com.infopush.app.ui.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.infopush.app.ui.favorites.FavoriteFeedbackViewModel
import com.infopush.app.ui.favorites.FavoritesScreen
import com.infopush.app.ui.favorites.UiEvent
import com.infopush.app.ui.feed.FeedScreen
import com.infopush.app.ui.messages.MessagesScreen
import com.infopush.app.ui.sources.SourcesScreen
import com.infopush.app.ui.settings.SettingsScreen

object AppRoute {
    const val FEED = "feed"
    const val SOURCES = "sources"
    const val FAVORITES = "favorites"
    const val MESSAGES = "messages"
    const val SETTINGS = "settings"
}

@Composable
fun AppNav() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val favoriteViewModel = remember { FavoriteFeedbackViewModel() }

    LaunchedEffect(favoriteViewModel) {
        favoriteViewModel.events.collect { event ->
            if (event is UiEvent.Toast) {
                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    NavHost(navController = navController, startDestination = AppRoute.FEED) {
        composable(AppRoute.FEED) {
            FeedScreen(onGoToSources = { navController.navigate(AppRoute.SOURCES) })
        }
        composable(AppRoute.SOURCES) {
            SourcesScreen(onGoToFavorites = { navController.navigate(AppRoute.FAVORITES) })
        }
        composable(AppRoute.FAVORITES) {
            FavoritesScreen(
                viewModel = favoriteViewModel,
                onGoToMessages = { navController.navigate(AppRoute.MESSAGES) }
            )
        }
        composable(AppRoute.MESSAGES) {
            MessagesScreen(onGoToSettings = { navController.navigate(AppRoute.SETTINGS) })
        }
        composable(AppRoute.SETTINGS) {
            SettingsScreen(
                onExportData = {},
                onImportReplace = {},
                onImportMerge = {}
            )
        }
    }
}
