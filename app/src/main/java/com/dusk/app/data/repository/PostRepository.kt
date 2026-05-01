package com.dusk.app.data.repository

import com.dusk.app.domain.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun getFeedPosts(): Flow<List<Post>>
    fun getUserPosts(uid: String): Flow<List<Post>>
    suspend fun getPost(postId: String): Result<Post>
    suspend fun createPost(post: Post): Result<String>
    suspend fun likePost(postId: String): Result<Unit>
    suspend fun unlikePost(postId: String): Result<Unit>
    suspend fun bookmarkPost(postId: String): Result<Unit>
    suspend fun unbookmarkPost(postId: String): Result<Unit>
    suspend fun deletePost(postId: String): Result<Unit>
    fun getTrendingPosts(): Flow<List<Post>>
}
