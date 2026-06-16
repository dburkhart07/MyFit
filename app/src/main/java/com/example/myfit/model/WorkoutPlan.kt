package com.example.myfit.model

/**
 * 1. What: The strict, type-safe domain model of a generated weekly workout plan, built from
 *    [DayPlan]s and [Exercise]s. Mirrors how [OnboardingPreferences] is the strict counterpart
 *    to its DTO.
 * 2. Who: Produced by the workout generator / repository, consumed by WorkoutsViewModel and the
 *    Workouts UI; mapped to/from [com.example.myfit.model.dto.WorkoutPlanDto] for Firestore.
 * 3. When: Built when a plan is generated or loaded; persisted under the user's document.
 *
 * Invariants are enforced in init so a malformed plan can't exist as a domain object — invalid
 * data is rejected at the mapper boundary instead (toDomain returns null).
 */
data class WorkoutPlan(
    val days: List<DayPlan>,
    val generatedAt: Long = 0L,
) {
    init {
        require(days.size == DAYS_IN_WEEK) { "a plan must have exactly $DAYS_IN_WEEK days, was ${days.size}" }
    }

    val isStale: Boolean
        get() = generatedAt > 0L &&
                System.currentTimeMillis() - generatedAt > SEVEN_DAYS_MILLIS

    companion object {
        const val DAYS_IN_WEEK = 7
        const val SEVEN_DAYS_MILLIS = 7L * 24 * 60 * 60 * 1000
        val DAY_ORDER = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    }
}

/**
 * 1. What: One day of the plan — either a rest day or a workout day with a focus and exercises.
 * 2. Who: Held by [WorkoutPlan]; rendered as a card (weekly view) and a detail page.
 * 3. When: Part of every generated/loaded plan.
 */
data class DayPlan(
    val day: String,        // one of WorkoutPlan.DAY_ORDER
    val focus: String,      // "Upper body", "Lower body", "Full body", "Rest", ...
    val isRest: Boolean,
    val exercises: List<Exercise> = emptyList(),
) {
    val title: String get() = "$day · $focus"
    val exerciseCountLabel: String get() = "${exercises.size} exercises"

    init {
        require(day in WorkoutPlan.DAY_ORDER) { "day must be one of ${WorkoutPlan.DAY_ORDER}, was $day" }
        if (isRest) {
            require(exercises.isEmpty()) { "a rest day must have no exercises" }
        }
    }
}

/**
 * 1. What: A single exercise with its target sets and reps.
 * 2. Who: Held by [DayPlan]; rendered as a row and checked off in the detail view.
 * 3. When: Part of every workout day in a plan.
 */
data class Exercise(
    val name: String,
    val sets: Int,
    val reps: Int,
) {
    init {
        require(name.isNotBlank()) { "exercise name must not be blank" }
        require(sets in 1..MAX_SETS) { "sets must be in 1..$MAX_SETS, was $sets" }
        require(reps in 1..MAX_REPS) { "reps must be in 1..$MAX_REPS, was $reps" }
    }

    companion object {
        const val MAX_SETS = 20
        const val MAX_REPS = 100
    }
}