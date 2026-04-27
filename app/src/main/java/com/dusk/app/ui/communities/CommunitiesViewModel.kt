package com.dusk.app.ui.communities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dusk.app.data.repository.Community
import com.dusk.app.data.repository.CommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CommunitiesUiState(
    val communities: List<Community> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class CommunitiesViewModel @Inject constructor(
    private val repository: CommunityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunitiesUiState())
    val uiState: StateFlow<CommunitiesUiState> = _uiState.asStateFlow()

    init { loadCommunities() }

    private fun loadCommunities() {
        viewModelScope.launch {
            repository.getCommunities().collect { communities ->
                _uiState.value = _uiState.value.copy(communities = communities, isLoading = false)
            }
        }
    }

    fun toggleJoin(community: Community) {
        viewModelScope.launch {
            if (community.isJoined) {
                repository.leaveCommunity(community.id)
            } else {
                repository.joinCommunity(community.id)
            }
        }
    }
}
