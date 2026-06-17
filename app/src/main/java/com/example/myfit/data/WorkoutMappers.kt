package com.example.myfit.data

import com.example.myfit.model.DayPlan
import com.example.myfit.model.DayRecord
import com.example.myfit.model.Exercise
import com.example.myfit.model.WeekRecord
import com.example.myfit.model.WorkoutPlan
import com.example.myfit.model.dto.DayPlanDto
import com.example.myfit.model.dto.DayRecordDto
import com.example.myfit.model.dto.ExerciseDto
import com.example.myfit.model.dto.WeekRecordDto
import com.example.myfit.model.dto.WorkoutPlanDto
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Mappings between the strict [WorkoutPlan] domain model and the Firestore-friendly
 * [WorkoutPlanDto]. Domain → DTO is total; DTO → domain returns null when the stored data can't
 * form a valid domain object (so a malformed/legacy document is treated as "no plan").
 *
 * Note: the DTO field is named [DayPlanDto.restDay] (NOT isRest). Firestore's reflection mapper
 * strips the "is" prefix from Boolean getters, so an `isRest` field round-trips under the key
 * "rest" and fails to deserialize. Using `restDay` avoids that. The domain model keeps `isRest`.
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
            exercises = day.exercises.map { ExerciseDto(it.name, it.sets, it.reps, it.done) },
            completedAt = day.completedAt,
        )
    },
    generatedAt = generatedAt,
    startDate = startDate.toEpochDay(),
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
    WorkoutPlan(
        days = mappedDays,
        generatedAt = generatedAt,
        startDate = resolveStartDate(),
    )
}.getOrNull()

/**
 * The plan's start date: the stored [WorkoutPlanDto.startDate] (epoch-day) when present, else a
 * best-effort fallback for legacy documents written before dates existed — the local date of
 * [WorkoutPlanDto.generatedAt], or today if that's missing too.
 */
private fun WorkoutPlanDto.resolveStartDate(): LocalDate = when {
    startDate > 0L -> LocalDate.ofEpochDay(startDate)
    generatedAt > 0L -> Instant.ofEpochMilli(generatedAt).atZone(ZoneId.systemDefault()).toLocalDate()
    else -> LocalDate.now()
}

/**
 * 1. What: Maps a single [DayPlanDto] to a domain [DayPlan], normalizing a rest day to carry no
 *    exercises; returns null if the day fails the domain invariants.
 * 2. Who: Used by [WorkoutPlanDto.toDomain], and by the generator when parsing a single
 *    adjusted day from the model's JSON.
 * 3. When: While mapping each day of a stored/parsed plan, or a single re-prompted day.
 */
internal fun DayPlanDto.toDomain(): DayPlan? = runCatching {
    DayPlan(
        day = day,
        focus = focus.ifBlank { if (restDay) "Rest" else "Workout" },
        isRest = restDay,
        exercises = if (restDay) emptyList() else exercises.mapNotNull { it.toDomain() },
        completedAt = if (restDay) null else completedAt,
    )
}.getOrNull()

/**
 * 1. What: Maps a single [ExerciseDto] to a domain [Exercise]; returns null if it violates the
 *    name/sets/reps invariants.
 * 2. Who: Used when mapping a workout day's exercises.
 * 3. When: While mapping each exercise of a stored/parsed plan.
 */
private fun ExerciseDto.toDomain(): Exercise? =
    runCatching { Exercise(name = name, sets = sets, reps = reps, done = done) }.getOrNull()

/* ----------------------------- Workout history ----------------------------- */

/**
 * 1. What: Condenses a finished [WorkoutPlan] into an archivable [WeekRecord] — workout days only
 *    (rest days dropped), each carrying how many of its exercises were checked off.
 * 2. Who: Called by [WorkoutRepositoryImpl] just before a new week overwrites the current plan.
 * 3. When: When the user starts a new week.
 */
fun WorkoutPlan.toWeekRecord(): WeekRecord = WeekRecord(
    startDate = startDate,
    days = days.filterNot { it.isRest }.map { day ->
        DayRecord(
            day = day.day,
            focus = day.focus,
            completed = day.exercises.count { it.done },
            total = day.exercises.size,
        )
    },
)

/**
 * 1. What: Maps an archived [WeekRecord] to its Firestore DTO.
 * 2. Who: Called by [WorkoutRepositoryImpl] before writing a history document.
 * 3. When: When archiving a finished week.
 */
fun WeekRecord.toDto(): WeekRecordDto = WeekRecordDto(
    startDate = startDate.toEpochDay(),
    days = days.map { DayRecordDto(it.day, it.focus, it.completed, it.total) },
)

/**
 * 1. What: Maps a Firestore [WeekRecordDto] back to a domain [WeekRecord].
 * 2. Who: Called by [WorkoutRepositoryImpl] after reading the history subcollection.
 * 3. When: When the History tab loads.
 */
fun WeekRecordDto.toDomain(): WeekRecord = WeekRecord(
    startDate = LocalDate.ofEpochDay(startDate),
    days = days.map { DayRecord(it.day, it.focus, it.completed, it.total) },
)