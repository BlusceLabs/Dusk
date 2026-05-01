package com.dusk.app.domain.model

data class Post(
    val id: String = "",
    val authorId: String = "",
    val author: DuskUser? = null,
    val content: String = "",
    val images: List<String> = emptyList(),
    val video: String? = null,
    val likes: Int = 0,
    val comments: Int = 0,
    val reposts: Int = 0,
    val bookmarks: Int = 0,
    val isLiked: Boolean = false,
    val isReposted: Boolean = false,
    val isBookmarked: Boolean = false,
    val type: PostType = PostType.POST,
    val tags: List<String> = emptyList(),
    val createdAt: Long = 0L
)

enum class PostType { POST, REEL, STORY, LIVE }
