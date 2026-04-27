package com.dusk.app.data.repository

import com.dusk.app.domain.model.Post
import kotlinx.coroutines.flow.Flow

interface ReelsRepository {
    fun getReels(): Flow<List<Post>>
    suspend fun likeReel(reelId: String): Result<Unit>
    suspend fun unlikeReel(reelId: String): Result<Unit>
    suspend fun getReelComments(reelId: String): Result<List<ReelComment>>
    suspend fun addComment(reelId: String, text: String): Result<Unit>
}

data class ReelComment(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val avatar: String = "",
    val text: String = "",
    val likes: Int = 0,
    val createdAt: Long = 0L
)
