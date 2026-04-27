package com.dusk.app.data.repository

import com.dusk.app.domain.model.DuskUser
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUser(uid: String): Flow<DuskUser?>
    suspend fun updateProfile(displayName: String, bio: String, avatar: String): Result<Unit>
    suspend fun followUser(uid: String): Result<Unit>
    suspend fun unfollowUser(uid: String): Result<Unit>
    fun searchUsers(query: String): Flow<List<DuskUser>>
}
