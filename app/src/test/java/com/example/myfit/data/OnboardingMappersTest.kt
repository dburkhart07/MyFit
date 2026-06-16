package com.example.myfit.data

import com.example.myfit.model.Equipment
import com.example.myfit.model.ExperienceLevel
import com.example.myfit.model.Goal
import com.example.myfit.model.OnboardingPreferences
import com.example.myfit.model.dto.OnboardingPreferencesDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Round-trip and edge-case tests for the onboarding domain <-> DTO mappings, which hold
 * the only branching logic in the data layer.
 */
class OnboardingMappersTest {

    @Test
    fun domain_to_dto_to_domain_roundTrips() {
        val prefs = OnboardingPreferences(
            goal = Goal.LOSE_WEIGHT,
            daysPerWeek = 5,
            equipment = listOf(Equipment.DUMBBELLS, Equipment.BANDS),
            experience = ExperienceLevel.INTERMEDIATE,
        )

        assertEquals(prefs, prefs.toDto().toDomain())
    }

    @Test
    fun toDto_storesHumanReadableLabels() {
        val dto = OnboardingPreferences(
            goal = Goal.BUILD_MUSCLE,
            daysPerWeek = 3,
            equipment = listOf(Equipment.BODYWEIGHT),
            experience = ExperienceLevel.BEGINNER,
        ).toDto()

        assertEquals("Build muscle", dto.goal)
        assertEquals(listOf("Bodyweight"), dto.equipment)
        assertEquals("Beginner", dto.experience)
    }

    @Test
    fun toDomain_dropsUnknownEquipmentLabels() {
        val dto = OnboardingPreferencesDto(
            goal = "Build muscle",
            daysPerWeek = 4,
            equipment = listOf("Dumbbells", "Spaceship"),
            experience = "Advanced",
        )

        assertEquals(listOf(Equipment.DUMBBELLS), dto.toDomain()?.equipment)
    }

    @Test
    fun toDomain_returnsNull_whenGoalUnknown() {
        val dto = OnboardingPreferencesDto(
            goal = "Become a wizard",
            daysPerWeek = 4,
            equipment = listOf("Dumbbells"),
            experience = "Advanced",
        )

        assertNull(dto.toDomain())
    }

    @Test
    fun toDomain_returnsNull_whenNoKnownEquipment() {
        val dto = OnboardingPreferencesDto(
            goal = "Build muscle",
            daysPerWeek = 4,
            equipment = listOf("Spaceship"),
            experience = "Advanced",
        )

        assertNull(dto.toDomain())
    }

    @Test
    fun toDomain_returnsNull_whenDaysOutOfRange() {
        val dto = OnboardingPreferencesDto(
            goal = "Build muscle",
            daysPerWeek = 9,
            equipment = listOf("Dumbbells"),
            experience = "Advanced",
        )

        assertNull(dto.toDomain())
    }
}
