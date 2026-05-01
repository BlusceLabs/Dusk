package com.dusk.app.data.repository

import com.dusk.app.domain.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : PostRepository {

    override fun getFeedPosts(): Flow<List<Post>> = callbackFlow {
        val listener = firestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<Post>()?.copy(id = doc.id)
                } ?: emptyList()
                trySend(posts)
            }
        awaitClose { listener.remove() }
    }

    override fun getUserPosts(uid: String): Flow<List<Post>> = callbackFlow {
        val listener = firestore.collection("posts")
            .whereEqualTo("authorId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<Post>()?.copy(id = doc.id)
                } ?: emptyList()
                trySend(posts)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getPost(postId: String): Result<Post> = runCatching {
        val doc = firestore.collection("posts").document(postId).get().await()
        doc.toObject<Post>()?.copy(id = doc.id) ?: throw Exception("Post not found")
    }

    override suspend fun createPost(post: Post): Result<String> = runCatching {
        val docRef = firestore.collection("posts").document()
        docRef.set(post.copy(id = docRef.id)).await()
        docRef.id
    }

    override suspend fun likePost(postId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        firestore.collection("posts").document(postId)
            .update("likes", FieldValue.increment(1)).await()
        firestore.collection("likes").document("${uid}_$postId")
            .set(mapOf("uid" to uid, "postId" to postId, "createdAt" to System.currentTimeMillis())).await()
    }

    override suspend fun unlikePost(postId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        firestore.collection("posts").document(postId)
            .update("likes", FieldValue.increment(-1)).await()
        firestore.collection("likes").document("${uid}_$postId").delete().await()
    }

    override suspend fun bookmarkPost(postId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        firestore.collection("bookmarks").document("${uid}_$postId")
            .set(mapOf("uid" to uid, "postId" to postId)).await()
    }

    override suspend fun unbookmarkPost(postId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        firestore.collection("bookmarks").document("${uid}_$postId").delete().await()
    }

    override suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        firestore.collection("posts").document(postId).delete().await()
    }

    override fun getTrendingPosts(): Flow<List<Post>> = callbackFlow {
        val listener = firestore.collection("posts")
            .orderBy("likes", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<Post>()?.copy(id = doc.id)
                } ?: emptyList()
                trySend(posts)
            }
        awaitClose { listener.remove() }
    }
}
