package com.dusk.app.domain.model

data class DuskUser(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val avatar: String = "",
    val banner: String = "",
    val followers: Int = 0,
    val following: Int = 0,
    val isVerified: Boolean = false,
    val isCreator: Boolean = false,
    val isPremium: Boolean = false,
    val createdAt: Long = 0L
)
