package com.dusk.app.data.repository

import com.dusk.app.domain.model.Story
import kotlinx.coroutines.flow.Flow

interface StoryRepository {
    fun getActiveStories(): Flow<List<Story>>
    fun getUserStories(userId: String): Flow<List<Story>>
    suspend fun uploadStory(imageUrl: String): Result<Unit>
    suspend fun markViewed(storyId: String): Result<Unit>
    suspend fun deleteStory(storyId: String): Result<Unit>
}
