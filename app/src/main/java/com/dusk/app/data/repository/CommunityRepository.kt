package com.dusk.app.data.repository

import com.dusk.app.domain.model.DuskUser
import kotlinx.coroutines.flow.Flow

data class Community(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val avatar: String = "",
    val banner: String = "",
    val memberCount: Int = 0,
    val isPrivate: Boolean = false,
    val isJoined: Boolean = false,
    val createdAt: Long = 0L,
    val ownerId: String = ""
)

data class CommunityPost(
    val id: String = "",
    val communityId: String = "",
    val author: DuskUser? = null,
    val content: String = "",
    val images: List<String> = emptyList(),
    val likes: Int = 0,
    val comments: Int = 0,
    val createdAt: Long = 0L
)

interface CommunityRepository {
    fun getCommunities(): Flow<List<Community>>
    fun getCommunityPosts(communityId: String): Flow<List<CommunityPost>>
    suspend fun joinCommunity(communityId: String): Result<Unit>
    suspend fun leaveCommunity(communityId: String): Result<Unit>
    suspend fun createCommunity(name: String, description: String, isPrivate: Boolean): Result<Community>
}
