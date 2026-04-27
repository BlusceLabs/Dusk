package com.dusk.app.ui.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dusk.app.data.repository.LiveRepository
import com.dusk.app.data.repository.LiveStream
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LiveUiState(
    val streams: List<LiveStream> = emptyList(),
    val isLoading: Boolean = true,
    val viewerStreamId: String? = null
)

@HiltViewModel
class LiveViewModel @Inject constructor(
    private val repository: LiveRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveUiState())
    val uiState: StateFlow<LiveUiState> = _uiState.asStateFlow()

    init { loadStreams() }

    private fun loadStreams() {
        viewModelScope.launch {
            repository.getActiveStreams().collect { streams ->
                _uiState.value = _uiState.value.copy(streams = streams, isLoading = false)
            }
        }
    }

    fun startStream(title: String, thumbnailUrl: String) {
        viewModelScope.launch {
            repository.startStream(title, thumbnailUrl)
        }
    }

    fun endStream(streamId: String) {
        viewModelScope.launch {
            repository.endStream(streamId)
        }
    }

    fun watchStream(streamId: String) {
        _uiState.value = _uiState.value.copy(viewerStreamId = streamId)
    }

    fun closeViewer() {
        _uiState.value = _uiState.value.copy(viewerStreamId = null)
    }
}
