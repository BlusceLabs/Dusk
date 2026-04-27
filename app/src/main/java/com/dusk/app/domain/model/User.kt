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
</file_content>

<file_content path="/home/mantra/Dusk/app/src/main/java/com/dusk/app/domain/model/Post.kt">
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
</file_content>

<file_content path="/home/mantra/Dusk/app/src/main/java/com/dusk/app/domain/model/Chat.kt">
package com.dusk.app.domain.model

data class Chat(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageAt: Long = 0L,
    val lastSenderId: String = "",
    val unreadCount: Int = 0,
    val isGroup: Boolean = false,
    val groupName: String? = null,
    val groupAvatar: String? = null
)

data class ChatMessage(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val text: String = "",
    val image: String? = null,
    val createdAt: Long = 0L,
    val readBy: List<String> = emptyList()
)
</file_content>

<file_content path="/home/mantra/Dusk/app/src/main/java/com/dusk/app/domain/model/Notification.kt">
package com.dusk.app.domain.model

data class Notification(
    val id: String = "",
    val type: NotificationType = NotificationType.LIKE,
    val fromUserId: String = "",
    val fromUser: DuskUser? = null,
    val targetId: String = "",
    val content: String = "",
    val read: Boolean = false,
    val createdAt: Long = 0L
)

enum class NotificationType {
    LIKE, COMMENT, FOLLOW, REPOST, MENTION, STREAM_START, PREMIUM_GIFT
}
</file_content>

<file_content path="/home/mantra/Dusk/app/src/main/java/com/dusk/app/domain/model/Story.kt">
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