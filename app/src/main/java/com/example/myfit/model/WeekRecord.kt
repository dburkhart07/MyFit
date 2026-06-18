package com.example.myfit.model

import java.time.LocalDate

/**
 * 1. What: An archived, finished week of training — a lightweight completion summary kept for the
 *    History tab. Only workout days are recorded (rest days are omitted), each with how many of its
 *    exercises were checked off.
 * 2. Who: Produced when a new week is generated (the prior week is archived), read back by the
 *    History screen; mapped to/from [com.example.myfit.model.dto.WeekRecordDto] for Firestore.
 * 3. When: Written once per week at week's end; read when the History tab loads.
 */
data class WeekRecord(
    val startDate: LocalDate,
    val days: List<DayRecord>,
)

/**
 * 1. What: One workout day's archived result — its label, focus, and how many of [total] exercises
 *    were [completed]. The [DayStatus] (and the History icon) is derived from those counts.
 * 2. Who: Held by [WeekRecord]; rendered as a row in the week-detail view.
 * 3. When: Part of every archived week.
 */
data class DayRecord(
    val day: String,
    val focus: String,
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
}
