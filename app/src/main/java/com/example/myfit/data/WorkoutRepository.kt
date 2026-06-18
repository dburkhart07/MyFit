package com.example.myfit.data

import com.example.myfit.model.DayPlan
import com.example.myfit.model.DifficultyAdjustment
import com.example.myfit.model.ProfileStats
import com.example.myfit.model.WeekRecord
import com.example.myfit.model.WorkoutPlan
import java.time.LocalDate

/**
 * 1. What: The contract for persisting, loading, and generating the user's weekly workout plan.
 * 2. Who: Implemented by [WorkoutRepositoryImpl] (Firestore + AI); consumed by WorkoutsViewModel.
 *    Mirrors [OnboardingRepository].
 * 3. When: Called from a coroutine when the Workouts tab needs a plan (load), when one must be
 *    created (generate), or when the user edits/completes a plan (save).
 */
interface WorkoutRepository {

    /**
     * 1. What: Loads the current user's saved plan, or null if none/invalid is stored.
     * 2. Who: Implemented by [WorkoutRepositoryImpl]; called by WorkoutsViewModel.
     * 3. When: When the Workouts tab first appears.
     */
    suspend fun getPlan(): Result<WorkoutPlan?>

    /**
     * 1. What: Persists [plan] to the current user's document.
     * 2. Who: Implemented by [WorkoutRepositoryImpl]; called by WorkoutsViewModel.
     * 3. When: After a plan is generated, or after the user edits it (swap/delete/add).
     */
    suspend fun savePlan(plan: WorkoutPlan): Result<Unit>

    /**
     * 1. What: Generates a fresh plan starting today (falling back to a mock if the AI call fails),
     *    stamps it with the current time and start date, saves it, and returns it. Use this for a
     *    brand-new week.
     * 2. Who: Implemented by [WorkoutRepositoryImpl]; called by WorkoutsViewModel.
     * 3. When: When there's no saved plan, or the user picks "generate a whole new week".
     */
    suspend fun generateAndSavePlan(): Result<WorkoutPlan>

    /**
     * 1. What: Generates a fresh plan anchored at [startDate] WITHOUT saving it. The caller decides
     *    what to keep/merge and persists the result (used to regenerate only the remaining days of
     *    the current week).
     * 2. Who: Implemented by [WorkoutRepositoryImpl]; called by WorkoutsViewModel.
     * 3. When: When the user picks "redo current week".
     */
    suspend fun generatePlan(startDate: LocalDate): Result<WorkoutPlan>

    /**
     * 1. What: Produces a [direction]-adjusted version of [day] (same exercises, shifted sets/reps)
     *    via the AI, falling back to a deterministic nudge so it always succeeds. The caller is
     *    responsible for splicing the result into the plan and saving it.
     * 2. Who: Implemented by [WorkoutRepositoryImpl]; called by WorkoutsViewModel.
     * 3. When: When the user picks "Make it easier" / "Make it harder" on a workout day.
     */
    suspend fun adjustDayDifficulty(day: DayPlan, direction: DifficultyAdjustment): Result<DayPlan>

    /**
     * 1. What: Reads the user's archived weeks, most recent first.
     * 2. Who: Implemented by [WorkoutRepositoryImpl]; called by HistoryViewModel.
     * 3. When: When the History tab loads.
     */
    suspend fun getHistory(): Result<List<WeekRecord>>

    /**
     * 1. What: Computes the profile stats — total fully-completed workouts (across history and the
     *    current week) and the number of weeks in which every workout was fully completed.
     * 2. Who: Implemented by [WorkoutRepositoryImpl]; called by AccountStatsViewModel.
     * 3. When: When the Account tab loads.
     */
    suspend fun getProfileStats(): Result<ProfileStats>
}
