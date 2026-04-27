package com.dusk.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dusk.app.data.repository.PostRepository
import com.dusk.app.data.repository.UserRepository
import com.dusk.app.domain.model.DuskUser
import com.dusk.app.domain.model.Post
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: DuskUser? = null,
    val posts: List<Post> = emptyList(),
    val isCurrentUser: Boolean = false,
    val isFollowing: Boolean = false,
    val isLoading: Boolean = true,
    val selectedTab: Int = 0,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var currentUserId: String = ""

    fun loadProfile(userId: String, currentUserId: String) {
        this.currentUserId = currentUserId
        _uiState.update { it.copy(isCurrentUser = userId == currentUserId) }

        viewModelScope.launch {
            userRepository.getUser(userId).collect { user ->
                _uiState.update { it.copy(user = user, isLoading = false) }
            }
        }

        viewModelScope.launch {
            postRepository.getUserPosts(userId).collect { posts ->
                _uiState.update { it.copy(posts = posts) }
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun toggleFollow() {
        val targetUser = _uiState.value.user?.uid ?: return
        viewModelScope.launch {
            if (_uiState.value.isFollowing) {
                userRepository.unfollowUser(targetUser)
                _uiState.update { it.copy(isFollowing = false) }
            } else {
                userRepository.followUser(targetUser)
                _uiState.update { it.copy(isFollowing = true) }
            }
        }
    }

    fun updateProfile(displayName: String, bio: String, avatar: String) {
        viewModelScope.launch {
            userRepository.updateProfile(displayName, bio, avatar)
        }
    }
}
