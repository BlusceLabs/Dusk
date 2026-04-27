package com.dusk.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dusk.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val username: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun onEmailChanged(email: String) { _uiState.value = _uiState.value.copy(email = email, error = null) }
    fun onPasswordChanged(password: String) { _uiState.value = _uiState.value.copy(password = password, error = null) }
    fun onUsernameChanged(username: String) { _uiState.value = _uiState.value.copy(username = username, error = null) }

    fun signIn(onSuccess: () -> Unit) {
        val s = _uiState.value
        if (s.email.isBlank() || s.password.isBlank()) return
        _uiState.value = s.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val result = authRepository.signIn(s.email, s.password)
            result.fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false); onSuccess() },
                onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
            )
        }
    }

    fun signUp(onSuccess: () -> Unit) {
        val s = _uiState.value
        if (s.email.isBlank() || s.password.isBlank()) return
        _uiState.value = s.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val result = authRepository.signUp(s.email, s.password, s.username)
            result.fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false); onSuccess() },
                onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
            )
        }
    }

    fun resetPassword(onSent: () -> Unit) {
        val s = _uiState.value
        if (s.email.isBlank()) return
        _uiState.value = s.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val result = authRepository.resetPassword(s.email)
            result.fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false); onSent() },
                onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
            )
        }
    }
}
