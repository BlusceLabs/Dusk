package com.dusk.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dusk.app.data.repository.ChatRepository
import com.dusk.app.domain.model.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatDetailUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentUserId: String = ""
)

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatDetailUiState())
    val uiState: StateFlow<ChatDetailUiState> = _uiState.asStateFlow()

    private var currentChatId: String = ""

    fun loadChat(chatId: String) {
        currentChatId = chatId
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
        _uiState.update { it.copy(currentUserId = uid) }
        viewModelScope.launch {
            chatRepository.getMessages(chatId).collect { messages ->
                _uiState.update { it.copy(messages = messages, isLoading = false) }
            }
        }
        viewModelScope.launch {
            chatRepository.markAsRead(chatId)
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || currentChatId.isEmpty()) return
        viewModelScope.launch {
            chatRepository.sendMessage(currentChatId, text)
        }
    }
}
