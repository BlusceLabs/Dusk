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

        firebaseUser.updateProfile(
            UserProfileChangeRequest.Builder().setDisplayName(displayName).build()
        ).await()

        val duskUser = DuskUser(
            uid = firebaseUser.uid,
            email = email,
            username = username,
            displayName = displayName,
            createdAt = System.currentTimeMillis()
        )
        firestore.collection("users").document(firebaseUser.uid)
            .set(duskUser, SetOptions.merge()).await()

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

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).await()
    }
}
