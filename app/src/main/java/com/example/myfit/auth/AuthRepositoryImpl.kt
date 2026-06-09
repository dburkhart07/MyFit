package com.example.myfit.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : AuthRepository {

    override suspend fun signUp(email: String, pass: String, name: String): Result<UserProfile> {
        return try {
            // 1. Create the Firebase Auth user.
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, pass).await()
            val firebaseUser = authResult.user ?: throw IllegalStateException("User ID is null")
            val userId = firebaseUser.uid

            // 2. Set the Auth display name so getCurrentUser() can greet the user on relaunch
            //    without needing a Firestore read.
            firebaseUser.updateProfile(
                UserProfileChangeRequest.Builder().setDisplayName(name).build()
            ).await()

            // 3. Persist the profile to Firestore at users/{id}.
            val userProfile = UserProfile(id = userId, email = email, name = name)
            firestore.collection("users").document(userId).set(userProfile).await()

            Result.success(userProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signIn(email: String, pass: String): Result<UserProfile> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, pass).await()
            val userId = authResult.user?.uid ?: throw IllegalStateException("User ID is null")

            // Fetch the latest profile data from Firestore.
            val document = firestore.collection("users").document(userId).get().await()
            val userProfile = document.toObject(UserProfile::class.java)
                ?: throw IllegalStateException("User profile not found")

            Result.success(userProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    override fun getCurrentUser(): UserProfile? {
        // Uses only the locally-cached Auth session (no network), so it's safe to call
        // synchronously when choosing the start destination on launch.
        val firebaseUser = firebaseAuth.currentUser ?: return null
        return UserProfile(
            id = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            name = firebaseUser.displayName ?: "",
        )
    }
}
