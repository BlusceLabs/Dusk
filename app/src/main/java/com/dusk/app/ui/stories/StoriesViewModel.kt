package com.dusk.app.ui.stories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dusk.app.data.repository.StoryRepository
import com.dusk.app.domain.model.Story
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StoriesUiState(
    val stories: List<Story> = emptyList(),
    val groupedStories: Map<String, List<Story>> = emptyMap(),
    val isLoading: Boolean = true,
    val currentStoryIndex: Int = 0,
    val currentGroupIndex: Int = 0,
    val viewerVisible: Boolean = false,
    val viewerProgress: Float = 0f
)

@HiltViewModel
class StoriesViewModel @Inject constructor(
    private val repository: StoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoriesUiState())
    val uiState: StateFlow<StoriesUiState> = _uiState.asStateFlow()

    init { loadStories() }

    private fun loadStories() {
        viewModelScope.launch {
            repository.getStories().collect { stories ->
                val grouped = stories.groupBy { it.userId }
                _uiState.value = _uiState.value.copy(
                    stories = stories,
                    groupedStories = grouped,
                    isLoading = false
                )
            }
        }
    }

    fun openViewer(userIndex: Int) {
        _uiState.value = _uiState.value.copy(
            viewerVisible = true,
            currentGroupIndex = userIndex,
            currentStoryIndex = 0,
            viewerProgress = 0f
        )
        startProgress()
    }

    private fun startProgress() {
        viewModelScope.launch {
            val groupKeys = _uiState.value.groupedStories.keys.toList()
            if (groupKeys.isEmpty()) return@launch
            val currentGroup = groupKeys[_uiState.value.currentGroupIndex]
            val groupStories = _uiState.value.groupedStories[currentGroup] ?: return@launch
            val currentStory = groupStories.getOrNull(_uiState.value.currentStoryIndex) ?: return@launch

            repository.markViewed(currentStory.id)

            val duration = if (currentStory.type.name == "VIDEO") 15000L else currentStory.duration.toLong()
            val steps = 100
            val stepMs = duration / steps

            for (i in 1..steps) {
                delay(stepMs)
                _uiState.value = _uiState.value.copy(viewerProgress = i.toFloat() / steps)
            }
            nextStory()
        }
    }

    fun nextStory() {
        val state = _uiState.value
        val groupKeys = state.groupedStories.keys.toList()
        if (groupKeys.isEmpty()) return
        val currentGroup = groupKeys[state.currentGroupIndex]
        val groupStories = state.groupedStories[currentGroup] ?: return

        if (state.currentStoryIndex + 1 < groupStories.size) {
            _uiState.value = state.copy(
                currentStoryIndex = state.currentStoryIndex + 1,
                viewerProgress = 0f
            )
            startProgress()
        } else if (state.currentGroupIndex + 1 < groupKeys.size) {
            _uiState.value = state.copy(
                currentGroupIndex = state.currentGroupIndex + 1,
                currentStoryIndex = 0,
                viewerProgress = 0f
            )
            startProgress()
        } else {
            closeViewer()
        }
    }

    fun previousStory() {
        val state = _uiState.value
        if (state.currentStoryIndex > 0) {
            _uiState.value = state.copy(
                currentStoryIndex = state.currentStoryIndex - 1,
                viewerProgress = 0f
            )
            startProgress()
        } else if (state.currentGroupIndex > 0) {
            _uiState.value = state.copy(
                currentGroupIndex = state.currentGroupIndex - 1,
                currentStoryIndex = 0,
                viewerProgress = 0f
            )
            startProgress()
        }
    }

    fun closeViewer() {
        _uiState.value = _uiState.value.copy(viewerVisible = false, viewerProgress = 0f)
    }
}
