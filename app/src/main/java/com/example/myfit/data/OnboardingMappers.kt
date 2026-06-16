package com.example.myfit.data

import com.example.myfit.model.Equipment
import com.example.myfit.model.ExperienceLevel
import com.example.myfit.model.Goal
import com.example.myfit.model.OnboardingPreferences
import com.example.myfit.model.dto.OnboardingPreferencesDto

/**
 * Mappings between the strict [OnboardingPreferences] domain model and the
 * Firestore-friendly [OnboardingPreferencesDto]. Enums serialize to their labels;
 * deserialization drops any label that no longer maps to a known enum.
 */

/**
 * 1. What: Maps the strict domain [OnboardingPreferences] to its Firestore DTO, flattening
 *    each enum to its stored label.
 * 2. Who: Called by the data layer ([OnboardingRepositoryImpl]) before a write.
 * 3. When: On save, just before the document is written to Firestore.
 */
fun OnboardingPreferences.toDto(): OnboardingPreferencesDto = OnboardingPreferencesDto(
    goal = goal.label,
    daysPerWeek = daysPerWeek,
    equipment = equipment.map { it.label },
    experience = experience.label,
)

/**
 * 1. What: Maps a Firestore [OnboardingPreferencesDto] back to the strict domain model,
 *    returning null when the stored data can't form a valid one (unknown goal/experience
 *    label, no recognized equipment, or out-of-range days).
 * 2. Who: Called by the data layer ([OnboardingRepositoryImpl]) after a read; null lets
 *    callers treat a malformed/legacy document as "no preferences" rather than crashing.
 * 3. When: On load, right after the document is fetched from Firestore.
 */
fun OnboardingPreferencesDto.toDomain(): OnboardingPreferences? {
    val goal = Goal.fromLabel(goal) ?: return null
    val experience = ExperienceLevel.fromLabel(experience) ?: return null
    val equipment = equipment.mapNotNull { Equipment.fromLabel(it) }
    if (equipment.isEmpty()) return null
    if (daysPerWeek !in OnboardingPreferences.DAYS_RANGE) return null
    return OnboardingPreferences(
        goal = goal,
        daysPerWeek = daysPerWeek,
        equipment = equipment,
        experience = experience,
    )
}
