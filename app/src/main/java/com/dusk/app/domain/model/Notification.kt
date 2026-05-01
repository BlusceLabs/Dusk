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
