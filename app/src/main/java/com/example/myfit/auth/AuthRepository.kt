package com.example.myfit.auth

interface AuthRepository {
    suspend fun signUp(
        email: String,
        pass: String,
        firstName: String,
        lastName: String,
    ): Result<UserProfile>
    suspend fun signIn(email: String, pass: String): Result<UserProfile>
    suspend fun signOut()
    fun getCurrentUser(): UserProfile?
}
