package com.dusk.app.data.repository

import com.dusk.app.domain.model.Chat
import com.dusk.app.domain.model.ChatMessage
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
class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ChatRepository {

    override fun getConversations(): Flow<List<Chat>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(emptyList()); return@callbackFlow }
        val listener = firestore.collection("chats")
            .whereArrayContains("participants", uid)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val chats = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<Chat>()?.copy(id = doc.id)
                } ?: emptyList()
                trySend(chats)
            }
        awaitClose { listener.remove() }
    }

    override fun getMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = firestore.collection("chats").document(chatId).collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<ChatMessage>()?.copy(id = doc.id)
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun sendMessage(chatId: String, text: String, image: String?): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        val msg = mapOf(
            "chatId" to chatId,
            "senderId" to uid,
            "text" to text,
            "image" to image,
            "createdAt" to System.currentTimeMillis(),
            "readBy" to listOf(uid)
        )
        firestore.collection("chats").document(chatId).collection("messages").add(msg).await()
        firestore.collection("chats").document(chatId).update(
            mapOf("lastMessage" to text, "lastMessageAt" to System.currentTimeMillis(), "lastSenderId" to uid)
        ).await()
    }

    override suspend fun createConversation(participantIds: List<String>, groupName: String?): Result<String> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        val allParticipants = (participantIds + uid).distinct()
        val chat = mapOf(
            "participants" to allParticipants,
            "lastMessage" to "",
            "lastMessageAt" to System.currentTimeMillis(),
            "lastSenderId" to uid,
            "unreadCount" to 0,
            "isGroup" to (groupName != null),
            "groupName" to groupName,
            "groupAvatar" to null
        )
        val docRef = firestore.collection("chats").document()
        docRef.set(chat).await()
        docRef.id
    }

    override suspend fun markAsRead(chatId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        firestore.collection("chats").document(chatId)
            .update("unreadCount", 0).await()
    }
}
