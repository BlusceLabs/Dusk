package com.dusk.app.data.repository

import com.dusk.app.domain.model.DuskUser
import kotlinx.coroutines.flow.Flow

data class LiveStream(
    val id: String = "",
    val userId: String = "",
    val user: DuskUser? = null,
    val title: String = "",
    val thumbnailUrl: String = "",
    val viewerCount: Int = 0,
    val startedAt: Long = 0L,
    val status: String = "live" // live, ended
)

interface LiveRepository {
    fun getActiveStreams(): Flow<List<LiveStream>>
    suspend fun startStream(title: String, thumbnailUrl: String): Result<LiveStream>
    suspend fun endStream(streamId: String): Result<Unit>
    suspend fun updateViewerCount(streamId: String, count: Int): Result<Unit>
}
