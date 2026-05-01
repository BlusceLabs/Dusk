package com.dusk.app.ui.auth

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dusk.app.data.repository.AuthRepository
import com.dusk.app.domain.model.DuskUser
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val email: String = "",
    val password: String = "",
    val username: String = "",
    val displayName: String = "",
    val isSignUp: Boolean = false,
    val passwordResetSent: Boolean = false
)

sealed class AuthState {
    object Loading : AuthState()
    data class Authenticated(val user: DuskUser) : AuthState()
    object Unauthenticated : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val googleSignInClient: GoogleSignInClient
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.isAuthenticated.collect { authenticated ->
                _authState.value = if (authenticated) {
                    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    val user = uid?.let { authRepository.getUserProfile(it).getOrNull() }
                    if (user != null) AuthState.Authenticated(user) else AuthState.Unauthenticated
                } else {
                    AuthState.Unauthenticated
                }
            }
        }
    }

    fun toggleMode() {
        _uiState.update {
            it.copy(
                isSignUp = !it.isSignUp,
                error = null,
                email = "",
                password = "",
                username = "",
                displayName = "",
                passwordResetSent = false
            )
        }
    }

    fun onEmailChanged(email: String) { _uiState.update { it.copy(email = email, error = null) } }
    fun onPasswordChanged(password: String) { _uiState.update { it.copy(password = password, error = null) } }
    fun onUsernameChanged(username: String) { _uiState.update { it.copy(username = username, error = null) } }
    fun onDisplayNameChanged(displayName: String) { _uiState.update { it.copy(displayName = displayName, error = null) } }

    fun signIn() {
        val s = _uiState.value
        if (s.email.isBlank() || s.password.isBlank()) {
            _uiState.update { it.copy(error = "Email and password are required") }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.loginUser(s.email, s.password)
            result.fold(
                onSuccess = {
                    val user = authRepository.getUserProfile(it.uid).getOrNull()
                    _authState.value = AuthState.Authenticated(user ?: it)
                },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message ?: "Sign in failed") } }
            )
        }
    }

    fun signUp() {
        val s = _uiState.value
        if (s.email.isBlank() || s.password.isBlank() || s.username.isBlank()) {
            _uiState.update { it.copy(error = "All fields are required") }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val name = s.displayName.ifBlank { s.username }
            val result = authRepository.registerUser(s.email, s.password, s.username, name)
            result.fold(
                onSuccess = { _authState.value = AuthState.Authenticated(it) },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message ?: "Sign up failed") } }
            )
        }
    }

    fun sendPasswordReset() {
        val s = _uiState.value
        if (s.email.isBlank()) {
            _uiState.update { it.copy(error = "Enter your email first") }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(s.email)
            result.fold(
                onSuccess = { _uiState.update { it.copy(isLoading = false, passwordResetSent = true) } },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to send reset email") } }
            )
        }
    }

    fun getGoogleSignInIntent(): Intent = googleSignInClient.signInIntent

    fun signInWithGoogle(data: Intent?) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Google sign-in failed: no token") }
                    return@launch
                }
                val result = authRepository.signInWithGoogle(idToken)
                result.fold(
                    onSuccess = { _authState.value = AuthState.Authenticated(it) },
                    onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message ?: "Google sign-in failed") } }
                )
            } catch (e: ApiException) {
                _uiState.update { it.copy(isLoading = false, error = "Google sign-in cancelled or failed") }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _authState.value = AuthState.Unauthenticated
            _uiState.value = AuthUiState()
        }
    }
}
