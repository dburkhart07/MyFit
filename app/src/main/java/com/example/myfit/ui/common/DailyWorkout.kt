package com.example.myfit.ui.common

import com.example.myfit.ui.history.WeekHistory

/**
 * 1. What: One completed (or missed) workout within a week.
 * 2. Who: Held by [WeekHistory]; rendered as a row in the week-detail view.
 * 3. When: Shown after the user taps a week card to drill into its days.
 */
data class DailyWorkout(
    val day: String,
    val title: String,
    val exercises: String,
    val completed: Boolean,
)