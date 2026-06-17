package com.example.myfit.model.dto

/**
 * 1. What: The Firestore-serializable shape of an archived week (one document per finished week,
 *    keyed by the week's start epoch-day) in the `workoutHistory` subcollection.
 * 2. Who: Written/read by the data layer; mapped to/from [com.example.myfit.model.WeekRecord].
 * 3. When: Serialized when a week is archived; deserialized when History loads.
 *
 * Every field has a default and a no-arg constructor, which Firestore requires for automatic
 * (de)serialization — the same DTO pattern as [WorkoutPlanDto].
 */
data class WeekRecordDto(
    val startDate: Long = 0L,  // epoch-day of the week's first day (also the document id)
    val days: List<DayRecordDto> = emptyList(),
)

/**
 * 1. What: Firestore-serializable shape of a single archived workout day.
 * 2. Who: Held by [WeekRecordDto]; mapped to/from [com.example.myfit.model.DayRecord].
 * 3. When: (De)serialized as part of an archived week.
 */
data class DayRecordDto(
    val day: String = "",
    val focus: String = "",
    val completed: Int = 0,
    val total: Int = 0,
)
