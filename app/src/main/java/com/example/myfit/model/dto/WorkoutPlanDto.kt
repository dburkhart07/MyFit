package com.example.myfit.model.dto

/**
 * 1. What: The Firestore-serializable shape of a generated workout plan.
 * 2. Who: Written to / read from `users/{uid}.workoutPlan` by the data layer; mapped to and from
 *    the strict [com.example.myfit.model.WorkoutPlan] domain model.
 * 3. When: Serialized on save and deserialized via Firestore's `toObject()` / `get(...)`.
 *
 * Every field is a primitive / list of primitives with a default and a no-arg constructor, which
 * Firestore requires for automatic (de)serialization. This is the same DTO pattern as
 * [OnboardingPreferencesDto].
 */
data class WorkoutPlanDto(
    val days: List<DayPlanDto> = emptyList(),
    val generatedAt: Long = 0L,
    val startDate: Long = 0L,  // epoch-day of days[0]; 0 = legacy doc (mapper falls back)
)

/**
 * 1. What: Firestore-serializable shape of a single day in the plan.
 * 2. Who: Held by [WorkoutPlanDto]; mapped to/from [com.example.myfit.model.DayPlan].
 * 3. When: (De)serialized as part of the plan document.
 */
data class DayPlanDto(
    val day: String = "",
    val focus: String = "",
    val restDay: Boolean = false,
    val exercises: List<ExerciseDto> = emptyList(),
    val completedAt: Long? = null,  // millis the workout was finished; null = not attempted
)

/**
 * 1. What: Firestore-serializable shape of a single exercise.
 * 2. Who: Held by [DayPlanDto]; mapped to/from [com.example.myfit.model.Exercise].
 * 3. When: (De)serialized as part of the plan document.
 */
data class ExerciseDto(
    val name: String = "",
    val sets: Int = 0,
    val reps: Int = 0,
    val done: Boolean = false,  // whether this exercise was checked off
)

