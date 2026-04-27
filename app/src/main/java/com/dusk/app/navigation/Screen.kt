package com.dusk.app.navigation

sealed class Screen(val route: String) {
    object SignIn : Screen("sign_in")
    object SignUp : Screen("sign_up")
    object ForgotPassword : Screen("forgot_password")
    object Feed : Screen("feed")
    object Profile : Screen("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }
    object Chat : Screen("chat")
    object ChatDetail : Screen("chat/{chatId}") {
        fun createRoute(chatId: String) = "chat/$chatId"
    }
    object Search : Screen("search")
    object Reels : Screen("reels")
    object Notifications : Screen("notifications")
    object Communities : Screen("communities")
    object Stories : Screen("stories/{userId}") {
        fun createRoute(userId: String) = "stories/$userId"
    }
    object Live : Screen("live/{streamId}") {
        fun createRoute(streamId: String) = "live/$streamId"
    }
    object Premium : Screen("premium")
    object Wallet : Screen("wallet")
    object Settings : Screen("settings")
    object PostDetail : Screen("post/{postId}") {
        fun createRoute(postId: String) = "post/$postId"
    }
}
