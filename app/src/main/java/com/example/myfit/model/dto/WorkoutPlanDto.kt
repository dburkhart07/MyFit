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
)

