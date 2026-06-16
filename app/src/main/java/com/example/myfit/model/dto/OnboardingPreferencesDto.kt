package com.example.myfit.model.dto

/**
 * 1. What: The Firestore-serializable shape of a user's onboarding preferences.
 * 2. Who: Written to / read from `users/{uid}.preferences` by the data layer; mapped
 *    to and from the strict [com.example.myfit.model.OnboardingPreferences] domain model.
 * 3. When: Serialized on save and deserialized via Firestore's `toObject()`.
 *
 * Fields are primitives with defaults and a no-arg constructor (the empty defaults),
 * which Firestore requires for automatic (de)serialization. Enums are stored as their
 * human-readable labels so the document stays readable in the console.
 */
data class OnboardingPreferencesDto(
    val goal: String = "",
    val daysPerWeek: Int = 0,
    val equipment: List<String> = emptyList(),
    val experience: String = "",
)