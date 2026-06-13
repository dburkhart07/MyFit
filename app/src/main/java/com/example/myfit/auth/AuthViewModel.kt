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
    private var profile: UserProfile? = repository.getCurrentUser()

    val displayName: String
        get() = profile?.let { "${it.firstName} ${it.lastName}".trim() }
            ?.takeIf { it.isNotBlank() }
            ?: "there"

    fun signUp(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        confirmPassword: String,
    ) {
        val trimmedFirst = firstName.trim()
        val trimmedLast = lastName.trim()
        val trimmedEmail = email.trim()
        val error = when {
            trimmedFirst.isBlank() -> "First name is required"
            trimmedLast.isBlank() -> "Last name is required"
            !isValidEmail(trimmedEmail) -> "Please enter a valid email address"
            password.length < MIN_PASSWORD_LENGTH -> "Password must be at least $MIN_PASSWORD_LENGTH characters"
            password != confirmPassword -> "Passwords do not match"
            else -> null
        }
        if (error != null) {
            _uiState.value = AuthUiState.Error(error)
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            repository.signUp(trimmedEmail, password, trimmedFirst, trimmedLast)
                .onSuccess {
                    profile = it
                    _uiState.value = AuthUiState.Success
                }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Sign up failed") }
        }
    }

    fun login(email: String, password: String) {
        val trimmedEmail = email.trim()
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            repository.signIn(trimmedEmail, password)
                .onSuccess {
                    profile = it
                    _uiState.value = AuthUiState.Success
                }
                // Catch-all for all errors
                .onFailure { _uiState.value = AuthUiState.Error("Credentials are invalid") }
        }
    }

    fun logout() {
        viewModelScope.launch { repository.signOut() }
        profile = null
        _uiState.value = AuthUiState.Idle
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    companion object {
        const val MIN_PASSWORD_LENGTH = 8
        private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
        fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())
    }
}
