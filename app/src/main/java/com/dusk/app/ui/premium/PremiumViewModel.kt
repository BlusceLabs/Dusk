package com.dusk.app.ui.premium

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dusk.app.data.repository.PremiumRepository
import com.dusk.app.data.repository.PremiumSubscription
import com.dusk.app.data.repository.PremiumTier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PremiumUiState(
    val tiers: List<PremiumTier> = emptyList(),
    val currentSubscription: PremiumSubscription? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val repository: PremiumRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PremiumUiState())
    val uiState: StateFlow<PremiumUiState> = _uiState.asStateFlow()

    init {
        loadTiers()
        loadSubscription()
    }

    private fun loadTiers() {
        viewModelScope.launch {
            repository.getTiers().collect { tiers ->
                _uiState.value = _uiState.value.copy(tiers = tiers, isLoading = false)
            }
        }
    }

    private fun loadSubscription() {
        viewModelScope.launch {
            val sub = repository.getCurrentSubscription().getOrNull()
            _uiState.value = _uiState.value.copy(currentSubscription = sub)
        }
    }

    fun subscribe(tierId: String) {
        viewModelScope.launch {
            repository.subscribe(tierId, "stripe")
            loadSubscription()
        }
    }

    fun cancelSubscription() {
        viewModelScope.launch {
            repository.cancelSubscription()
            loadSubscription()
        }
    }
}
