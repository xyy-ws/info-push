package com.infopush.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
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
import com.infopush.app.ui.settings.SettingsViewModel
import com.infopush.app.ui.sources.SourceDraft
import com.infopush.app.ui.sources.SourcesScreen
import com.infopush.app.ui.sources.SourcesViewModel

object AppRoute {
    const val FEED = "feed"
    const val SOURCES = "sources"
    const val FAVORITES = "favorites"
    const val MESSAGES = "messages"
    const val SETTINGS = "settings"
}

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit
)

@Composable
fun AppNav() {
    val navController = rememberNavController()
    val context = LocalContext.current

    val database = remember {
        Room.databaseBuilder(context, InfoPushDatabase::class.java, "info_push.db")
            .fallbackToDestructiveMigration()
            .build()
    }
    val api = remember { NetworkModule.createApi(BuildConfig.API_BASE_URL) }
    val repository = remember { InfoPushRepository(database, api, BuildConfig.ENABLE_MOCK_FALLBACK) }

    val feedViewModel = remember {
        FeedViewModel(
            observeSources = { repository.observeSources() },
            observeFeed = { sourceId -> repository.observeFeed(sourceId) },
            refreshSourcesAndFeed = { repository.refreshSourcesAndFeed() },
            refreshSource = { sourceId -> repository.refreshSource(sourceId) },
            getPersistedSelectedSourceId = { repository.getSelectedSourceId() },
            persistSelectedSourceId = { sourceId -> repository.saveSelectedSourceId(sourceId) }
        )
    }
    val sourcesViewModel = remember {
        SourcesViewModel(
            observeSources = { repository.observeSources() },
            refreshSources = { repository.refreshSourcesAndFeed() },
            addSource = { draft: SourceDraft -> repository.addSource(draft.name, draft.url, draft.type, draft.tags) },
            setSourceEnabled = { sourceId, enabled -> repository.setSourceEnabled(sourceId, enabled) },
            deleteSource = { sourceId -> repository.deleteSource(sourceId) },
            discoverSources = { keyword -> repository.searchAiSources(keyword) },
            addAiSourceToLocal = { source -> repository.addAiSourceToLocal(source) }
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
    val settingsViewModel = remember {
        SettingsViewModel(
            exportLocalJson = { repository.exportLocalJson() },
            importLocalJson = { json, mode -> repository.importLocalJson(json, mode) }
        )
    }

    val bottomNavItems = remember {
        listOf(
            BottomNavItem(AppRoute.FEED, "首页", icon = { Icon(Icons.Outlined.Home, contentDescription = "首页") }),
            BottomNavItem(AppRoute.SOURCES, "信息源", icon = { Icon(Icons.Outlined.RssFeed, contentDescription = "信息源") }),
            BottomNavItem(AppRoute.FAVORITES, "收藏", icon = { Icon(Icons.Outlined.CollectionsBookmark, contentDescription = "收藏") }),
            BottomNavItem(AppRoute.SETTINGS, "设置", icon = { Icon(Icons.Outlined.Settings, contentDescription = "设置") })
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                            }
                        },
                        icon = item.icon,
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoute.FEED,
            modifier = Modifier.padding(innerPadding)
        ) {
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
                    onGoToSettings = { navController.navigate(AppRoute.SETTINGS) }
                )
            }
            composable(AppRoute.SETTINGS) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onGoToMessages = { navController.navigate(AppRoute.MESSAGES) }
                )
            }
            composable(AppRoute.MESSAGES) {
                MessagesScreen(
                    viewModel = messagesViewModel,
                    onGoToSettings = { navController.popBackStack() }
                )
            }
        }
    }
}
