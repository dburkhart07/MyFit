package com.example.myfit.ui.history

import com.example.myfit.ui.common.DailyWorkout

/**
 * 1. What: A week of training with its per-day completion summary.
 * 2. Who: Backs the History list; one card per entry.
 * 3. When: The list view shows the summary; tapping opens [dailyWorkouts].
 */
data class WeekHistory(
    val week: String,
    val completed: Int,
    val total: Int,
    val dailyWorkouts: List<DailyWorkout>,
)