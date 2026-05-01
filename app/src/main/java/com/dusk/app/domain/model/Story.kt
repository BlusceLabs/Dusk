package com.dusk.app.domain.model

data class Story(
    val id: String = "",
    val userId: String = "",
    val user: DuskUser? = null,
    val mediaUrl: String = "",
    val type: StoryType = StoryType.IMAGE,
    val duration: Int = 5000,
    val viewedBy: List<String> = emptyList(),
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L
)

enum class StoryType { IMAGE, VIDEO }
