package com.example.myfit.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfit.data.OnboardingRepository
import com.example.myfit.data.OnboardingRepositoryImpl
import com.example.myfit.model.OnboardingPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The states the onboarding save can be in, observed by the screen to drive the
 * confirm button and navigation.
 */
sealed interface OnboardingUiState {
    data object Idle : OnboardingUiState
    data object Saving : OnboardingUiState
    data object Saved : OnboardingUiState
    data class Error(val message: String) : OnboardingUiState
}

/**
 * The states of loading the stored preferences, observed by the Account screen.
 * [Loaded.preferences] is null when the user has not completed onboarding yet.
 */
sealed interface PreferencesUiState {
    data object Loading : PreferencesUiState
    data class Loaded(val preferences: OnboardingPreferences?) : PreferencesUiState
    data class Error(val message: String) : PreferencesUiState
}

/**
 * 1. What: Owns the onboarding-save flow — turns the user's selections into a domain
 *    model and persists them via [OnboardingRepository], exposing progress as UI state.
 * 2. Who: Created by the onboarding screen via `viewModel()`; mirrors AuthViewModel.
 * 3. When: [savePreferences] runs when the user confirms; the screen navigates once
 *    [uiState] reaches [OnboardingUiState.Saved].
 */
class OnboardingViewModel(
    private val repository: OnboardingRepository = OnboardingRepositoryImpl(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Idle)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _preferences = MutableStateFlow<PreferencesUiState>(PreferencesUiState.Loading)
    val preferences: StateFlow<PreferencesUiState> = _preferences.asStateFlow()

    /**
     * 1. What: Loads the signed-in user's stored preferences into [preferences].
     * 2. Who: Called by the Account screen.
     * 3. When: When the screen first appears, and on retry after a load error.
     */
    fun loadPreferences() {
        _preferences.value = PreferencesUiState.Loading
        viewModelScope.launch {
            repository.getPreferences()
                .onSuccess { _preferences.value = PreferencesUiState.Loaded(it) }
                .onFailure {
                    _preferences.value =
                        PreferencesUiState.Error(it.message ?: "Could not load your preferences")
                }
        }
    }

    /**
     * 1. What: Persists [prefs] via the repository, surfacing progress through [uiState]
     *    and refreshing [preferences] on success.
     * 2. Who: Called by the Onboarding screen (confirm) and the Account screen (update).
     * 3. When: When the user confirms onboarding or saves edited preferences.
     */
    fun savePreferences(prefs: OnboardingPreferences) {
        _uiState.value = OnboardingUiState.Saving
        viewModelScope.launch {
            repository.savePreferences(prefs)
                .onSuccess {
                    _preferences.value = PreferencesUiState.Loaded(prefs)
                    _uiState.value = OnboardingUiState.Saved
                }
                .onFailure {
                    _uiState.value =
                        OnboardingUiState.Error(it.message ?: "Could not save your preferences")
                }
        }
    }

    /**
     * 1. What: Resets [uiState] to Idle, clearing a previous save error or success.
     * 2. Who: Called by the Onboarding and Account screens.
     * 3. When: After a completed save is handled, or when the user cancels/retries.
     */
    fun resetState() {
        _uiState.value = OnboardingUiState.Idle
    }
}
