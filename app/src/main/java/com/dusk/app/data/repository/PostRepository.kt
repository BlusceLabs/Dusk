package com.dusk.app.data.repository

import com.dusk.app.domain.model.DuskUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<DuskUser?>
    val isAuthenticated: Flow<Boolean>
    
    suspend fun registerUser(email: String, password: String, username: String, displayName: String): Result<DuskUser>
    suspend fun loginUser(email: String, password: String): Result<DuskUser>
    suspend fun loginWithUsername(username: String, password: String): Result<DuskUser>
    suspend fun signInWithGoogle(idToken: String): Result<DuskUser>
    suspend fun signOut()
    suspend fun getUserProfile(uid: String): Result<DuskUser>
    suspend fun updateProfile(uid: String, updates: Map<String, Any>): Result<Unit>
}
</file_content>

<file_content path="/home/mantra/Dusk/app/src/main/java/com/dusk/app/data/repository/AuthRepositoryImpl.kt">
package com.dusk.app.data.repository

import com.dusk.app.domain.model.DuskUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override val currentUser: Flow<DuskUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // Fetch user profile from Firestore
                firestore.collection("users").document(user.uid)
                    .get()
                    .addOnSuccessListener { doc ->
                        val duskUser = doc.toObject(DuskUser::class.java)?.copy(uid = user.uid)
                            ?: DuskUser(
                                uid = user.uid,
                                email = user.email ?: "",
                                displayName = user.displayName ?: "",
                                avatar = user.photoUrl?.toString() ?: ""
                            )
                        trySend(duskUser)
                    }
                    .addOnFailureListener {
                        trySend(DuskUser(uid = user.uid, email = user.email ?: ""))
                    }
            } else {
                trySend(null)
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override val isAuthenticated: Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser != null)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun registerUser(
        email: String, password: String, username: String, displayName: String
    ): Result<DuskUser> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user ?: throw Exception("User creation failed")

        // Update Firebase Auth profile
        firebaseUser.updateProfile(
            UserProfileChangeRequest.Builder().setDisplayName(displayName).build()
        ).await()

        // Create user document in Firestore
        val duskUser = DuskUser(
            uid = firebaseUser.uid,
            email = email,
            username = username,
            displayName = displayName,
            createdAt = System.currentTimeMillis()
        )
        firestore.collection("users").document(firebaseUser.uid)
            .set(duskUser, SetOptions.merge()).await()

        // Create username index for login resolution
        firestore.collection("usernames").document(username.lowercase())
            .set(mapOf("uid" to firebaseUser.uid)).await()

        duskUser
    }

    override suspend fun loginUser(email: String, password: String): Result<DuskUser> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw Exception("Login failed")
        val doc = firestore.collection("users").document(user.uid).get().await()
        doc.toObject(DuskUser::class.java)?.copy(uid = user.uid)
            ?: DuskUser(uid = user.uid, email = user.email ?: "")
    }

    override suspend fun loginWithUsername(username: String, password: String): Result<DuskUser> = runCatching {
        // Resolve username to email
        val usernameDoc = firestore.collection("usernames")
            .document(username.lowercase()).get().await()
        val uid = usernameDoc.getString("uid") ?: throw Exception("Username not found")
        val userDoc = firestore.collection("users").document(uid).get().await()
        val email = userDoc.getString("email") ?: throw Exception("User has no email")
        loginUser(email, password).getOrThrow()
    }

    override suspend fun signInWithGoogle(idToken: String): Result<DuskUser> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val firebaseUser = result.user ?: throw Exception("Google sign-in failed")

        // Check if existing user or create new
        val doc = firestore.collection("users").document(firebaseUser.uid).get().await()
        if (doc.exists()) {
            doc.toObject(DuskUser::class.java)?.copy(uid = firebaseUser.uid)
                ?: DuskUser(uid = firebaseUser.uid)
        } else {
            val duskUser = DuskUser(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = firebaseUser.displayName ?: "",
                avatar = firebaseUser.photoUrl?.toString() ?: ""
            )
            firestore.collection("users").document(firebaseUser.uid)
                .set(duskUser).await()
            duskUser
        }
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override suspend fun getUserProfile(uid: String): Result<DuskUser> = runCatching {
        val doc = firestore.collection("users").document(uid).get().await()
        doc.toObject(DuskUser::class.java)?.copy(uid = uid)
            ?: throw Exception("User not found")
    }

    override suspend fun updateProfile(uid: String, updates: Map<String, Any>): Result<Unit> = runCatching {
        firestore.collection("users").document(uid)
            .update(updates).await()
    }
}
</file_content>

<file_content path="/home/mantra/Dusk/app/src/main/java/com/dusk/app/data/repository/PostRepository.kt">
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
}
</file_content>

<file_content path="/home/mantra/Dusk/app/src/main/java/com/dusk/app/data/repository/PostRepositoryImpl.kt">
package com.dusk.app.data.repository

import com.dusk.app.domain.model.DuskUser
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
                    val post = doc.toObject<Post>()?.copy(id = doc.id)
                    post
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
}