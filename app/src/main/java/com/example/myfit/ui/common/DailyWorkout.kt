package com.example.myfit.ui.common

import com.example.myfit.model.DayStatus
import com.example.myfit.ui.history.WeekHistory

/**
 * 1. What: One workout day's result within a week — its label/title and how many of [total]
 *    exercises were [completed]. The completion [status] and display [label] derive from those.
 * 2. Who: Held by [WeekHistory]; rendered as a row in the week-detail view.
 * 3. When: Shown after the user taps a week card to drill into its days.
 */
data class DailyWorkout(
    val day: String,
    val title: String,
    val completed: Int,
    val total: Int,
) {
    /** Fully done → [DayStatus.COMPLETED]; some done → [DayStatus.PARTIAL]; none → [DayStatus.MISSED]. */
    val status: DayStatus
        get() = when {
            total > 0 && completed == total -> DayStatus.COMPLETED
            completed > 0 -> DayStatus.PARTIAL
            else -> DayStatus.MISSED
        }

    /** "Missed" when nothing was done, otherwise "x / y exercises". */
    val label: String
        get() = if (status == DayStatus.MISSED) "Missed" else "$completed / $total exercises"
}
