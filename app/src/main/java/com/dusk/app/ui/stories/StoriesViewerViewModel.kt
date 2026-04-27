package com.dusk.app.ui.stories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dusk.app.data.repository.StoryRepository
import com.dusk.app.domain.model.Story
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StoriesViewerState(
    val stories: List<Story> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class StoriesViewerViewModel @Inject constructor(
    private val storyRepository: StoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StoriesViewerState())
    val state: StateFlow<StoriesViewerState> = _state

    fun loadStories(userId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val stories = storyRepository.getUserStories(userId).first()
            _state.value = _state.value.copy(stories = stories, isLoading = false)
        }
    }

    fun nextStory() {
        val current = _state.value
        if (current.currentIndex < current.stories.size - 1) {
            _state.value = current.copy(currentIndex = current.currentIndex + 1)
        }
    }

    fun previousStory() {
        val current = _state.value
        if (current.currentIndex > 0) {
            _state.value = current.copy(currentIndex = current.currentIndex - 1)
        }
    }
}
