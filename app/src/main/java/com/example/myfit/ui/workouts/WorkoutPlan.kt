package com.example.myfit.ui.workouts

/**
 * UI models + dummy data for the editable weekly plan shown on the Workouts tab.
 *
 * Note: this is intentionally separate from [com.example.myfit.ui.common.DailyWorkout], which is
 * the History feature's read-only summary model (exercises as a single String). Here we need a
 * real list of exercises with sets/reps so the detail screen can be checked off and edited.
 *
 * When the backend lands, map users/{uid}/weeks/{week}/days documents into these shapes.
 *
 * Author: Premkumar Chandrasekar — CS 5520 final project (June 2026).
 */

data class Exercise(
    val name: String,
    val sets: Int,
    val reps: Int,
)

data class DayPlan(
    val day: String,      // "Mon", "Tue", ...
    val focus: String,    // "Upper body", "Lower body", "Full body", or "Rest"
    val isRest: Boolean,
    val exercises: List<Exercise> = emptyList(),
) {
    val title: String get() = "$day · $focus"
    val exerciseCountLabel: String get() = "${exercises.size} exercises"
}

object WorkoutMockData {

    private val upperBody = listOf(
        Exercise("Bench press", 3, 10),
        Exercise("Rows", 3, 12),
        Exercise("Shoulder press", 3, 10),
        Exercise("Bicep curls", 3, 12),
        Exercise("Tricep dips", 3, 10),
    )

    private val lowerBody = listOf(
        Exercise("Squats", 4, 10),
        Exercise("Romanian deadlift", 3, 10),
        Exercise("Leg press", 3, 12),
        Exercise("Calf raises", 4, 15),
        Exercise("Lunges", 3, 12),
        Exercise("Leg curls", 3, 12),
    )

    private val fullBody = listOf(
        Exercise("Deadlift", 3, 8),
        Exercise("Pull-ups", 3, 8),
        Exercise("Overhead press", 3, 10),
        Exercise("Goblet squat", 3, 12),
        Exercise("Push-ups", 3, 15),
        Exercise("Plank", 3, 1),
        Exercise("Face pulls", 3, 15),
    )

    val week: List<DayPlan> = listOf(
        DayPlan("Mon", "Upper body", isRest = false, exercises = upperBody),
        DayPlan("Tue", "Rest", isRest = true),
        DayPlan("Wed", "Lower body", isRest = false, exercises = lowerBody),
        DayPlan("Thu", "Upper body", isRest = false, exercises = upperBody),
        DayPlan("Fri", "Rest", isRest = true),
        DayPlan("Sat", "Full body", isRest = false, exercises = fullBody),
        DayPlan("Sun", "Rest", isRest = true),
    )

    /**
     * 1. What: Builds a default workout to drop onto a day that was previously a rest day.
     * 2. Who: Called by WorkoutsScreen when the user taps "Add a workout".
     * 3. When: When the user adds a workout to a rest day.
     */
    fun newWorkoutFor(day: String) = DayPlan(
        day = day,
        focus = "New workout",
        isRest = false,
        exercises = upperBody,
    )
}