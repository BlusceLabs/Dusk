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
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
}
