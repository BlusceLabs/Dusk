package com.dusk.app.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dusk.app.data.repository.WalletBalance
import com.dusk.app.data.repository.WalletRepository
import com.dusk.app.data.repository.WalletTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WalletUiState(
    val balance: WalletBalance = WalletBalance(),
    val transactions: List<WalletTransaction> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val repository: WalletRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    init {
        loadBalance()
        loadTransactions()
    }

    private fun loadBalance() {
        viewModelScope.launch {
            val balance = repository.getBalance().getOrDefault(WalletBalance())
            _uiState.value = _uiState.value.copy(balance = balance)
        }
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            repository.getTransactions().collect { transactions ->
                _uiState.value = _uiState.value.copy(
                    transactions = transactions,
                    isLoading = false
                )
            }
        }
    }

    fun depositPaypal(amount: Double) {
        viewModelScope.launch {
            repository.initiatePaypalPayment(amount)
        }
    }
}
