package com.dusk.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dusk.app.ui.chat.ChatDetailScreen
import com.dusk.app.ui.chat.ChatListScreen
import com.dusk.app.ui.feed.FeedScreen
import com.dusk.app.ui.live.LiveScreen
import com.dusk.app.ui.notifications.NotificationsScreen
import com.dusk.app.ui.premium.PremiumScreen
import com.dusk.app.ui.profile.ProfileScreen
import com.dusk.app.ui.reels.ReelsScreen
import com.dusk.app.ui.search.SearchScreen
import com.dusk.app.ui.settings.SettingsScreen
import com.dusk.app.ui.stories.StoriesViewerScreen
import com.dusk.app.ui.wallet.WalletScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Feed.route,
        modifier = modifier
    ) {
        composable(Screen.Feed.route) {
            FeedScreen(
                onProfileClick = { userId -> navController.navigate(Screen.Profile.createRoute(userId)) },
                onChatClick = { navController.navigate(Screen.Chat.route) },
                onSearchClick = { navController.navigate(Screen.Search.route) },
                onPostClick = { postId -> navController.navigate(Screen.PostDetail.createRoute(postId)) }
            )
        }
        composable(
            Screen.Profile.route,
            listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            ProfileScreen(
                userId = userId,
                currentUserId = "",
                onBack = { navController.popBackStack() },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onPostClick = { postId -> navController.navigate(Screen.PostDetail.createRoute(postId)) }
            )
        }
        composable(Screen.Chat.route) {
            ChatListScreen(
                onBack = { navController.popBackStack() },
                onChatClick = { chatId -> navController.navigate(Screen.ChatDetail.createRoute(chatId)) },
                onNewChat = { }
            )
        }
        composable(
            Screen.ChatDetail.route,
            listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
            ChatDetailScreen(
                chatId = chatId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Search.route) {
            SearchScreen(
                onUserClick = { userId -> navController.navigate(Screen.Profile.createRoute(userId)) },
                onPostClick = { postId -> navController.navigate(Screen.PostDetail.createRoute(postId)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Reels.route) {
            ReelsScreen()
        }
        composable(Screen.Notifications.route) {
            NotificationsScreen()
        }
        composable(Screen.Communities.route) { /* TODO */ }
        composable(
            Screen.Stories.route,
            listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            StoriesViewerScreen(
                uiState = com.dusk.app.ui.stories.StoriesUiState(),
                onClose = { navController.popBackStack() },
                onNext = { },
                onPrevious = { }
            )
        }
        composable(
            Screen.Live.route,
            listOf(navArgument("streamId") { type = NavType.StringType })
        ) { backStackEntry ->
            val streamId = backStackEntry.arguments?.getString("streamId") ?: return@composable
            LiveScreen()
        }
        composable(Screen.Premium.route) {
            PremiumScreen()
        }
        composable(Screen.Wallet.route) {
            WalletScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Screen.PostDetail.route,
            listOf(navArgument("postId") { type = NavType.StringType })
        ) { /* TODO */ }
    }
}
