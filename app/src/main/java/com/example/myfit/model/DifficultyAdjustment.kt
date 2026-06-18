package com.example.myfit.model

/**
 * 1. What: The direction the user wants to nudge a day's workout — the two options behind the
 *    "Alter daily workout" dialog ([com.example.myfit.ui.workouts.WorkoutsScreen]).
 * 2. Who: Chosen in the UI, passed through the ViewModel/repository to the workout generator,
 *    and woven into the AI prompt via [label].
 * 3. When: When the user taps "Make it easier" / "Make it harder" on a workout day.
 */
enum class DifficultyAdjustment(val label: String) {
    EASIER("easier"),
    HARDER("harder"),
}
