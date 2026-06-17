package com.example.myfit.model

import java.time.LocalDate

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
 *
 * [startDate] is the calendar date of `days[0]`; every other day is that date plus its index, so
 * the week is a real run of seven consecutive dates (it can start on any weekday). [generatedAt]
 * stays the millis stamp of when the week was first created and drives [isStale].
 */
data class WorkoutPlan(
    val days: List<DayPlan>,
    val generatedAt: Long = 0L,
    val startDate: LocalDate = LocalDate.now(),
) {
    init {
        require(days.size == DAYS_IN_WEEK) { "a plan must have exactly $DAYS_IN_WEEK days, was ${days.size}" }
    }

    val isStale: Boolean
        get() = generatedAt > 0L &&
                System.currentTimeMillis() - generatedAt > SEVEN_DAYS_MILLIS

    /** The calendar date of the day at [index] (days[0] is [startDate]). */
    fun dateOf(index: Int): LocalDate = startDate.plusDays(index.toLong())

    /**
     * True once every day of the week is in the past (today is after the last day). A new week may
     * only be started once the current one is over, so each archived week is a full seven days.
     */
    fun isWeekOver(today: LocalDate): Boolean =
        today.isAfter(startDate.plusDays((DAYS_IN_WEEK - 1).toLong()))

    /**
     * Indices that a "redo current week" should regenerate: every future day, plus today itself
     * unless its workout has already been attempted ([DayPlan.completedAt] set). Empty when the
     * whole week is in the past — in which case redoing the current week makes no sense.
     */
    fun remainingDayIndices(today: LocalDate): List<Int> = days.indices.filter { i ->
        val date = dateOf(i)
        date.isAfter(today) || (date.isEqual(today) && days[i].completedAt == null)
    }

    companion object {
        const val DAYS_IN_WEEK = 7
        const val SEVEN_DAYS_MILLIS = 7L * 24 * 60 * 60 * 1000
        val DAY_ORDER = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        /** The [DAY_ORDER] label for a date's weekday (Mon=1 … Sun=7). */
        fun labelFor(date: LocalDate): String = DAY_ORDER[date.dayOfWeek.value - 1]

        /** The seven day labels, in order, for a week beginning on [startDate]. */
        fun orderedLabels(startDate: LocalDate): List<String> =
            (0 until DAYS_IN_WEEK).map { labelFor(startDate.plusDays(it.toLong())) }
    }
}

/**
 * How a workout day reads at a glance on its weekly card. Drives the card's border color.
 * [PENDING] = upcoming/not yet due (or a rest day); [COMPLETED] = every exercise checked off;
 * [PARTIAL] = some checked off; [MISSED] = a past day never attempted, or one finished with
 * nothing checked off.
 */
enum class DayStatus { PENDING, COMPLETED, PARTIAL, MISSED }

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
    val completedAt: Long? = null,  // millis when the user finished the workout; null = not attempted
) {
    val title: String get() = "$day · $focus"
    val exerciseCountLabel: String get() = "${exercises.size} exercises"

    init {
        require(day in WorkoutPlan.DAY_ORDER) { "day must be one of ${WorkoutPlan.DAY_ORDER}, was $day" }
        if (isRest) {
            require(exercises.isEmpty()) { "a rest day must have no exercises" }
        }
    }

    /**
     * This day's [DayStatus] given its calendar [date] and [today]. A rest day is always
     * [DayStatus.PENDING]; an attempted day is graded by how many exercises were checked off; an
     * un-attempted day is [DayStatus.MISSED] once its date is in the past, else [DayStatus.PENDING].
     */
    fun status(date: LocalDate, today: LocalDate): DayStatus {
        if (isRest) return DayStatus.PENDING
        return if (completedAt != null) {
            val done = exercises.count { it.done }
            when {
                done == exercises.size && exercises.isNotEmpty() -> DayStatus.COMPLETED
                done > 0 -> DayStatus.PARTIAL
                else -> DayStatus.MISSED
            }
        } else {
            if (date.isBefore(today)) DayStatus.MISSED else DayStatus.PENDING
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
    val done: Boolean = false,  // checked off during a live session; persisted on "Complete workout"
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