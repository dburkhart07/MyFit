package com.example.myfit.data

import com.example.myfit.model.DayPlan
import com.example.myfit.model.Exercise
import com.example.myfit.model.WorkoutPlan
import com.example.myfit.model.dto.DayPlanDto
import com.example.myfit.model.dto.ExerciseDto
import com.example.myfit.model.dto.WorkoutPlanDto

/**
 * Mappings between the strict [WorkoutPlan] domain model and the Firestore-friendly
 * [WorkoutPlanDto]. Domain → DTO is total, DTO → domain
 * returns null when the stored data can't form a valid domain object.
 */

/**
 * 1. What: Maps the strict domain [WorkoutPlan] to its Firestore DTO.
 * 2. Who: Called by the data layer ([WorkoutRepositoryImpl]) before a write.
 * 3. When: On save, just before the document is written to Firestore.
 */
fun WorkoutPlan.toDto(): WorkoutPlanDto = WorkoutPlanDto(
    days = days.map { day ->
        DayPlanDto(
            day = day.day,
            focus = day.focus,
            restDay = day.isRest,
            exercises = day.exercises.map { ExerciseDto(it.name, it.sets, it.reps) },
        )
    },
)

/**
 * 1. What: Maps a Firestore [WorkoutPlanDto] back to the strict domain model, returning null when
 *    the stored data can't form a valid one (wrong day count, bad day label, out-of-range
 *    sets/reps, or a rest day that somehow carries exercises).
 * 2. Who: Called by the data layer ([WorkoutRepositoryImpl]) after a read, and by the generator
 *    when validating the model's JSON output.
 * 3. When: On load (Firestore) or right after parsing the AI response.
 */
fun WorkoutPlanDto.toDomain(): WorkoutPlan? = runCatching {
    val mappedDays = days.map { it.toDomain() ?: return null }
    WorkoutPlan(days = mappedDays)
}.getOrNull()

/**
 * 1. What: Maps a single [DayPlanDto] to a domain [DayPlan], normalizing a rest day to carry no
 *    exercises; returns null if the day fails the domain invariants.
 * 2. Who: Used by [WorkoutPlanDto.toDomain].
 * 3. When: While mapping each day of a stored/parsed plan.
 */
private fun DayPlanDto.toDomain(): DayPlan? = runCatching {
    DayPlan(
        day = day,
        focus = focus.ifBlank { if (restDay) "Rest" else "Workout" },
        isRest = restDay,
        exercises = if (restDay) emptyList() else exercises.mapNotNull { it.toDomain() },
    )
}.getOrNull()

/**
 * 1. What: Maps a single [ExerciseDto] to a domain [Exercise]; returns null if it violates the
 *    name/sets/reps invariants.
 * 2. Who: Used when mapping a workout day's exercises.
 * 3. When: While mapping each exercise of a stored/parsed plan.
 */
private fun ExerciseDto.toDomain(): Exercise? =
    runCatching { Exercise(name = name, sets = sets, reps = reps) }.getOrNull()