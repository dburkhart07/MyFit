package com.example.myfit.data

import com.example.myfit.model.OnboardingPreferences
import com.example.myfit.model.dto.OnboardingPreferencesDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Firestore-backed [OnboardingRepository]. Stores preferences as a `preferences` map
 * field directly on the signed-in user's document (`users/{uid}`), using a merge write
 * so it never clobbers the profile fields (name/email) written at sign-up.
 */
class OnboardingRepositoryImpl(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : OnboardingRepository {

    /**
     * 1. What: Writes [prefs] as the `preferences` map on `users/{uid}`, merging so the
     *    profile fields are preserved; returns the outcome as a [Result].
     * 2. Who: The Firestore implementation of [OnboardingRepository.savePreferences].
     * 3. When: On onboarding confirm or a preferences update, from a coroutine.
     */
    override suspend fun savePreferences(prefs: OnboardingPreferences): Result<Unit> {
        return try {
            val uid = firebaseAuth.currentUser?.uid
                ?: throw IllegalStateException("No signed-in user")
            firestore.collection(USERS).document(uid)
                .set(mapOf(PREFERENCES to prefs.toDto()), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 1. What: Reads the `preferences` map from `users/{uid}` and maps it to the domain
     *    model (null if absent/invalid); returns the outcome as a [Result].
     * 2. Who: The Firestore implementation of [OnboardingRepository.getPreferences].
     * 3. When: When a screen requests the stored preferences, from a coroutine.
     */
    override suspend fun getPreferences(): Result<OnboardingPreferences?> {
        return try {
            val uid = firebaseAuth.currentUser?.uid
                ?: throw IllegalStateException("No signed-in user")
            val document = firestore.collection(USERS).document(uid).get().await()
            val dto = document.get(PREFERENCES, OnboardingPreferencesDto::class.java)
            Result.success(dto?.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Stores all collections to be accessed throughout document
    companion object {
        private const val USERS = "users"
        private const val PREFERENCES = "preferences"
    }
}
