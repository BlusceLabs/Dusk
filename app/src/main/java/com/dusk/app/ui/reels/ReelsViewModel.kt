package com.dusk.app.ui.reels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dusk.app.data.repository.ReelComment
import com.dusk.app.data.repository.ReelsRepository
import com.dusk.app.domain.model.Post
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReelsUiState(
    val reels: List<Post> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = true,
    val comments: List<ReelComment> = emptyList(),
    val showComments: Boolean = false,
    val selectedReel: Post? = null,
    val error: String? = null
)

@HiltViewModel
class ReelsViewModel @Inject constructor(
    private val reelsRepository: ReelsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReelsUiState())
    val uiState: StateFlow<ReelsUiState> = _uiState.asStateFlow()

    init {
        loadReels()
    }

    private fun loadReels() {
        viewModelScope.launch {
            reelsRepository.getReels().collect { reels ->
                _uiState.value = _uiState.value.copy(reels = reels, isLoading = false)
            }
        }
    }

    fun onReelVisible(index: Int) {
        _uiState.value = _uiState.value.copy(currentIndex = index)
    }

    fun toggleLike(reel: Post) {
        viewModelScope.launch {
            if (reel.isLiked) {
                reelsRepository.unlikeReel(reel.id)
            } else {
                reelsRepository.likeReel(reel.id)
            }
        }
    }

    fun openComments(reel: Post) {
        _uiState.value = _uiState.value.copy(showComments = true, selectedReel = reel)
        viewModelScope.launch {
            reelsRepository.getReelComments(reel.id).onSuccess { comments ->
                _uiState.value = _uiState.value.copy(comments = comments)
            }
        }
    }

    fun closeComments() {
        _uiState.value = _uiState.value.copy(showComments = false, comments = emptyList())
    }

    fun addComment(text: String) {
        val reel = _uiState.value.selectedReel ?: return
        viewModelScope.launch {
            reelsRepository.addComment(reel.id, text).onSuccess {
                reelsRepository.getReelComments(reel.id).onSuccess { comments ->
                    _uiState.value = _uiState.value.copy(comments = comments)
                }
            }
        }
    }
}
