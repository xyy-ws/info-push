package com.infopush.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.infopush.app.BuildConfig
import com.infopush.app.data.local.InfoPushDatabase
import com.infopush.app.data.remote.NetworkModule
import com.infopush.app.data.repo.InfoPushRepository
import com.infopush.app.ui.favorites.FavoritesScreen
import com.infopush.app.ui.favorites.FavoritesViewModel
import com.infopush.app.ui.feed.FeedScreen
import com.infopush.app.ui.feed.FeedViewModel
import com.infopush.app.ui.messages.MessagesScreen
import com.infopush.app.ui.messages.MessagesViewModel
import com.infopush.app.ui.settings.SettingsScreen
import com.infopush.app.ui.sources.SourcesScreen
import com.infopush.app.ui.sources.SourcesViewModel

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

    val database = remember {
        Room.databaseBuilder(context, InfoPushDatabase::class.java, "info_push.db").build()
    }
    val api = remember { NetworkModule.createApi(BuildConfig.API_BASE_URL) }
    val repository = remember { InfoPushRepository(database, api, BuildConfig.ENABLE_MOCK_FALLBACK) }

    val feedViewModel = remember {
        FeedViewModel(
            observeSources = { repository.observeSources() },
            observeFeed = { sourceId -> repository.observeFeed(sourceId) },
            refreshSourcesAndFeed = { repository.refreshSourcesAndFeed() }
        )
    }
    val sourcesViewModel = remember {
        SourcesViewModel(
            observeSources = { repository.observeSources() },
            refreshSources = { repository.refreshSourcesAndFeed() }
        )
    }
    val favoritesViewModel = remember {
        FavoritesViewModel(
            observeFavorites = { repository.observeFavorites() },
            refreshFavorites = { repository.refreshFavorites() }
        )
    }
    val messagesViewModel = remember {
        MessagesViewModel(
            observeMessages = { repository.observeMessages() },
            refreshMessages = { repository.refreshMessages() }
        )
    }

    NavHost(navController = navController, startDestination = AppRoute.FEED) {
        composable(AppRoute.FEED) {
            FeedScreen(
                viewModel = feedViewModel,
                onGoToSources = { navController.navigate(AppRoute.SOURCES) }
            )
        }
        composable(AppRoute.SOURCES) {
            SourcesScreen(
                viewModel = sourcesViewModel,
                onGoToFavorites = { navController.navigate(AppRoute.FAVORITES) }
            )
        }
        composable(AppRoute.FAVORITES) {
            FavoritesScreen(
                viewModel = favoritesViewModel,
                onGoToMessages = { navController.navigate(AppRoute.MESSAGES) }
            )
        }
        composable(AppRoute.MESSAGES) {
            MessagesScreen(
                viewModel = messagesViewModel,
                onGoToSettings = { navController.navigate(AppRoute.SETTINGS) }
            )
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
