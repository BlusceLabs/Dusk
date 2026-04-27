package com.dusk.app.data.repository

import com.dusk.app.domain.model.Chat
import com.dusk.app.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getConversations(): Flow<List<Chat>>
    fun getMessages(chatId: String): Flow<List<ChatMessage>>
    suspend fun sendMessage(chatId: String, text: String, image: String? = null): Result<Unit>
    suspend fun createConversation(participantIds: List<String>, groupName: String? = null): Result<String>
    suspend fun markAsRead(chatId: String): Result<Unit>
}
