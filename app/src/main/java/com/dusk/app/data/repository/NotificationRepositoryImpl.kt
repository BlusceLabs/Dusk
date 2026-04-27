package com.dusk.app.data.repository

import com.dusk.app.domain.model.Notification
import com.google.firebase.auth.FirebaseAuth
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
class NotificationRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : NotificationRepository {

    override fun getNotifications(): Flow<List<Notification>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(emptyList()); return@callbackFlow }
        val listener = firestore.collection("users").document(uid)
            .collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    val notifications = snapshots.documents.mapNotNull {
                        it.toObject<Notification>()?.copy(id = it.id)
                    }
                    trySend(notifications)
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        firestore.collection("users").document(uid)
            .collection("notifications").document(notificationId)
            .update("read", true).await()
    }

    override suspend fun markAllAsRead(): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        val unread = firestore.collection("users").document(uid)
            .collection("notifications")
            .whereEqualTo("read", false)
            .get().await()
        unread.documents.forEach { doc ->
            doc.reference.update("read", true).await()
        }
    }
}
