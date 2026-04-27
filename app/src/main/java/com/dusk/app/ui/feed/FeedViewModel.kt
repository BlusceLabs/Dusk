package com.dusk.app.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dusk.app.data.repository.AuthRepository
import com.dusk.app.data.repository.PostRepository
import com.dusk.app.data.repository.StoryRepository
import com.dusk.app.domain.model.DuskUser
import com.dusk.app.domain.model.Post
import com.dusk.app.domain.model.Story
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedUiState(
    val selectedTab: Int = 0,
    val posts: List<Post> = emptyList(),
    val stories: List<Story> = emptyList(),
    val currentUser: DuskUser? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val storyRepository: StoryRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        loadFeed()
    }

    private fun loadFeed() {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
        }

        viewModelScope.launch {
            postRepository.getFeedPosts().collect { posts ->
                _uiState.update { it.copy(posts = posts, isLoading = false, isRefreshing = false) }
            }
        }

        viewModelScope.launch {
            storyRepository.getActiveStories().collect { stories ->
                _uiState.update { it.copy(stories = stories) }
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun toggleLike(post: Post) {
        viewModelScope.launch {
            if (post.isLiked) {
                postRepository.unlikePost(post.id)
            } else {
                postRepository.likePost(post.id)
            }
        }
    }

    fun toggleBookmark(post: Post) {
        viewModelScope.launch {
            if (post.isBookmarked) {
                postRepository.unbookmarkPost(post.id)
            } else {
                postRepository.bookmarkPost(post.id)
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
    }
}
