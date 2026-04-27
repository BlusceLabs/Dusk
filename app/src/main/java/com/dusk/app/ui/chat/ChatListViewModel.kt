package com.dusk.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dusk.app.data.repository.ChatRepository
import com.dusk.app.domain.model.Chat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatListUiState(
    val conversations: List<Chat> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    init {
        loadConversations()
    }

    private fun loadConversations() {
        viewModelScope.launch {
            chatRepository.getConversations().collect { conversations ->
                _uiState.update { it.copy(conversations = conversations, isLoading = false) }
            }
        }
    }

    fun createConversation(userId: String) {
        viewModelScope.launch {
            chatRepository.createConversation(listOf(userId))
        }
    }
}
