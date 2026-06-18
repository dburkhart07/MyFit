package com.example.myfit.model

/**
 * 1. What: The headline numbers on the Account profile card — how many workouts the user has fully
 *    completed in total, and how many weeks they completed every single workout in.
 * 2. Who: Computed by the workout repository from the user's history + current plan; shown by
 *    [com.example.myfit.ui.account.AccountScreen].
 * 3. When: Loaded when the Account tab appears.
 */
data class ProfileStats(
    val completedWorkouts: Int,
    val weekStreak: Int,
)
