package com.example.myfit.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI state for the login / signup screens. */
sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object Success : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel(
    private val repository: AuthRepository = AuthRepositoryImpl(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** Current user profile, seeded from any existing session. */
    private var profile: UserProfile? = repository.getCurrentUser()

    /** True if a user session already exists (used to pick the start destination). */
    val isLoggedIn: Boolean
        get() = profile != null

    /** Display name to greet the user with; falls back to email, then a generic word. */
    val displayName: String
        get() = profile?.name?.takeIf { it.isNotBlank() }
            ?: profile?.email
            ?: "there"

    fun signUp(name: String, email: String, password: String) {
        if (!validate(name, email, password)) return
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            repository.signUp(email.trim(), password, name.trim())
                .onSuccess {
                    profile = it
                    _uiState.value = AuthUiState.Success
                }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Sign up failed") }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Email and password are required")
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            repository.signIn(email.trim(), password)
                .onSuccess {
                    profile = it
                    _uiState.value = AuthUiState.Success
                }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Login failed") }
        }
    }

    fun logout() {
        viewModelScope.launch { repository.signOut() }
        profile = null
        _uiState.value = AuthUiState.Idle
    }

    /** Reset transient state so a screen re-entry starts clean. */
    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    private fun validate(name: String, email: String, password: String): Boolean {
        val error = when {
            name.isBlank() -> "Name is required"
            email.isBlank() -> "Email is required"
            password.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }
        return if (error != null) {
            _uiState.value = AuthUiState.Error(error)
            false
        } else {
            true
        }
    }
}
