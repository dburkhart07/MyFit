package com.example.myfit.auth

/**
 * Authentication + user-profile operations. Per the MyFit architecture, account creation,
 * login, and session management live entirely on the client via Firebase Auth, while the
 * user's profile document is stored at `users/{id}` in Firestore.
 */
interface AuthRepository {
    suspend fun signUp(email: String, pass: String, name: String): Result<UserProfile>
    suspend fun signIn(email: String, pass: String): Result<UserProfile>
    suspend fun signOut()
    fun getCurrentUser(): UserProfile?
}
