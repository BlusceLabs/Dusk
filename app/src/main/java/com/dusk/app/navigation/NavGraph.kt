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
    isAuthenticated: Boolean,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = if (isAuthenticated) Screen.Feed.route else Screen.SignIn.route,
        modifier = modifier
    ) {
        composable(Screen.SignIn.route) { /* TODO SignInScreen */ }
        composable(Screen.SignUp.route) { /* TODO SignUpScreen */ }
        composable(Screen.ForgotPassword.route) { /* TODO ForgotPasswordScreen */ }
        composable(Screen.Feed.route) {
            FeedScreen(
                onProfileClick = { userId -> navController.navigate("profile/$userId") },
                onChatClick = { navController.navigate(Screen.Chat.route) },
                onSearchClick = { navController.navigate(Screen.Search.route) },
                onReelsClick = { navController.navigate(Screen.Reels.route) },
                onNotificationsClick = { navController.navigate(Screen.Notifications.route) }
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
                onPostClick = { postId -> navController.navigate("post/$postId") }
            )
        }
        composable(Screen.Chat.route) {
            ChatListScreen(
                onBack = { navController.popBackStack() },
                onChatClick = { chatId -> navController.navigate("chat/$chatId") },
                onNewChat = { /* TODO */ }
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
                onUserClick = { userId -> navController.navigate("profile/$userId") },
                onPostClick = { postId -> navController.navigate("post/$postId") },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Reels.route) {
            ReelsScreen(
                onBack = { navController.popBackStack() },
                onProfileClick = { userId -> navController.navigate("profile/$userId") }
            )
        }
        composable(Screen.Notifications.route) {
            NotificationsScreen(
                onBack = { navController.popBackStack() },
                onUserClick = { userId -> navController.navigate("profile/$userId") }
            )
        }
        composable(Screen.Communities.route) { /* TODO CommunitiesScreen */ }
        composable(
            Screen.Stories.route,
            listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            StoriesViewerScreen(
                userId = userId,
                onDismiss = { navController.popBackStack() }
            )
        }
        composable(
            Screen.Live.route,
            listOf(navArgument("streamId") { type = NavType.StringType })
        ) { backStackEntry ->
            val streamId = backStackEntry.arguments?.getString("streamId") ?: return@composable
            LiveScreen(
                streamId = streamId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Premium.route) {
            PremiumScreen(
                onBack = { navController.popBackStack() },
                onSubscribe = { /* TODO */ }
            )
        }
        composable(Screen.Wallet.route) {
            WalletScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Screen.PostDetail.route,
            listOf(navArgument("postId") { type = NavType.StringType })
        ) { /* TODO PostDetailScreen */ }
    }
}
