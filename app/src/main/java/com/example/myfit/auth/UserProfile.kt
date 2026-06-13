package com.example.myfit.auth

/**
 * Domain model for a MyFit user. Stored at `users/{id}` in Firestore.
 *
 * All fields have defaults so Firestore can deserialize via [com.google.firebase.firestore.DocumentSnapshot.toObject]
 * (which requires a no-arg constructor).
 */
data class UserProfile(
    val id: String = "",
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
)
