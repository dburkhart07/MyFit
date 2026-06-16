package com.example.myfit.model

/**
 * 1. What: The strict, type-safe domain model of a user's onboarding answers, together
 *    with the closed sets of choices ([Goal], [Equipment], [ExperienceLevel]) it is built from.
 * 2. Who: Produced by the onboarding screen, consumed by OnboardingViewModel and the
 *    data layer; mapped to/from [com.example.myfit.model.dto.OnboardingPreferencesDto] for Firestore storage.
 * 3. When: Built when the user confirms onboarding; persisted under the user's document.
 *
 * Unlike the DTO, every field is a closed type (enum / bounded Int), so invalid
 * combinations are unrepresentable once an instance exists.
 */
data class OnboardingPreferences(
    val goal: Goal,
    val daysPerWeek: Int,
    val equipment: List<Equipment>,
    val experience: ExperienceLevel,
) {
    init {
        require(daysPerWeek in DAYS_RANGE) { "daysPerWeek must be in $DAYS_RANGE, was $daysPerWeek" }
        require(equipment.isNotEmpty()) { "equipment must not be empty" }
    }

    companion object {
        val DAYS_RANGE = 1..7
    }
}

/**
 * The user's primary training goal — a single-select onboarding choice. [label] is the
 * human-readable value shown in the UI and persisted to Firestore.
 */
enum class Goal(val label: String) {
    BUILD_MUSCLE("Build muscle"),
    LOSE_WEIGHT("Lose weight"),
    GENERAL_FITNESS("General fitness"),
    IMPROVE_ENDURANCE("Improve endurance");

    companion object {
        /**
         * 1. What: Resolves a stored/display [label] back to its enum constant, or null if unknown.
         * 2. Who: Used by the data-layer mappers when deserializing a Firestore document.
         * 3. When: On load, turning stored label strings back into the strict domain enums.
         */
        fun fromLabel(label: String): Goal? = entries.firstOrNull { it.label == label }
    }
}

/**
 * A piece of equipment the user has access to — a multi-select onboarding choice. [label]
 * is the human-readable value shown in the UI and persisted to Firestore.
 */
enum class Equipment(val label: String) {
    DUMBBELLS("Dumbbells"),
    BODYWEIGHT("Bodyweight"),
    BANDS("Bands"),
    BARBELL("Barbell"),
    KETTLEBELL("Kettlebell"),
    NONE("None");

    companion object {
        /**
         * 1. What: Resolves a stored/display [label] back to its enum constant, or null if unknown.
         * 2. Who: Used by the data-layer mappers when deserializing a Firestore document.
         * 3. When: On load, turning stored label strings back into the strict domain enums.
         */
        fun fromLabel(label: String): Equipment? = entries.firstOrNull { it.label == label }
    }
}

/**
 * The user's self-reported training experience — a single-select onboarding choice. [label]
 * is the human-readable value shown in the UI and persisted to Firestore.
 */
enum class ExperienceLevel(val label: String) {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced"),
    EXPERT("Expert");

    companion object {
        /**
         * 1. What: Resolves a stored/display [label] back to its enum constant, or null if unknown.
         * 2. Who: Used by the data-layer mappers when deserializing a Firestore document.
         * 3. When: On load, turning stored label strings back into the strict domain enums.
         */
        fun fromLabel(label: String): ExperienceLevel? = entries.firstOrNull { it.label == label }
    }
}
