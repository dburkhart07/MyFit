package com.example.myfit.data

import com.example.myfit.model.OnboardingPreferences

/**
 * 1. What: The contract for persisting and loading a user's onboarding preferences.
 * 2. Who: Implemented by [OnboardingRepositoryImpl] (Firestore); consumed by
 *    OnboardingViewModel. Mirrors the AuthRepository pattern.
 * 3. When: Called from a coroutine when the user confirms onboarding (save) or when a
 *    screen needs the stored preferences (load).
 */
interface OnboardingRepository {

    /**
     * 1. What: Persists [prefs] to the current user's document.
     * 2. Who: Implemented by [OnboardingRepositoryImpl]; called by OnboardingViewModel.
     * 3. When: When the user confirms onboarding or updates preferences; fails if no user
     *    is signed in.
     */
    suspend fun savePreferences(prefs: OnboardingPreferences): Result<Unit>

    /**
     * 1. What: Loads the current user's preferences, or null if none/invalid are stored.
     * 2. Who: Implemented by [OnboardingRepositoryImpl]; called by OnboardingViewModel.
     * 3. When: When a screen (e.g. Account) needs the stored preferences; fails if no user
     *    is signed in.
     */
    suspend fun getPreferences(): Result<OnboardingPreferences?>
}
