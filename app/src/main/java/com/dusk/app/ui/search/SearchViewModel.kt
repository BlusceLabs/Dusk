package com.dusk.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dusk.app.data.repository.PostRepository
import com.dusk.app.data.repository.UserRepository
import com.dusk.app.domain.model.DuskUser
import com.dusk.app.domain.model.Post
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val users: List<DuskUser> = emptyList(),
    val trending: List<Post> = emptyList(),
    val tags: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    init {
        loadTrending()
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(users = emptyList(), isLoading = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300) // debounce
            _uiState.update { it.copy(isLoading = true) }
            userRepository.searchUsers(query).collect { users ->
                _uiState.update { it.copy(users = users, isLoading = false) }
            }
        }
    }

    private fun loadTrending() {
        viewModelScope.launch {
            postRepository.getTrendingPosts().collect { posts ->
                _uiState.update { it.copy(trending = posts) }
            }
        }
    }

    fun searchTag(tag: String) {
        _uiState.update { it.copy(query = "#$tag") }
    }
}
