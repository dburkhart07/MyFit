package com.example.myfit.data

import com.example.myfit.model.DayPlan
import com.example.myfit.model.DayStatus
import com.example.myfit.model.DifficultyAdjustment
import com.example.myfit.model.ProfileStats
import com.example.myfit.model.WeekRecord
import com.example.myfit.model.WorkoutPlan
import com.example.myfit.model.dto.WeekRecordDto
import com.example.myfit.model.dto.WorkoutPlanDto
import java.time.LocalDate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Firestore-backed [WorkoutRepository]. Stores the plan as a `workoutPlan` map field on the
 * signed-in user's document (`users/{uid}`), using a merge write so it never clobbers the
 * profile/preferences fields written elsewhere — exactly the pattern in [OnboardingRepositoryImpl].
 *
 * Generation delegates to a [WorkoutGenerator] (Gemini) and falls back to [MockWorkoutGenerator]
 * if that fails, so the user always ends up with a usable plan. The saved plan carries a
 * `generatedAt` timestamp so the UI can detect when a week has elapsed.
 */
class WorkoutRepositoryImpl(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val generator: WorkoutGenerator = GeminiWorkoutGenerator(),
    private val onboardingRepository: OnboardingRepository = OnboardingRepositoryImpl(),
) : WorkoutRepository {

    /**
     * 1. What: Reads the `workoutPlan` map from `users/{uid}` and maps it to the domain model
     *    (null if absent/invalid); returns the outcome as a [Result].
     * 2. Who: The Firestore implementation of [WorkoutRepository.getPlan].
     * 3. When: When the Workouts tab requests the stored plan, from a coroutine.
     */
    override suspend fun getPlan(): Result<WorkoutPlan?> {
        return try {
            val uid = requireUid()
            val document = firestore.collection(USERS).document(uid).get().await()
            val dto = document.get(WORKOUT_PLAN, WorkoutPlanDto::class.java)
            Result.success(dto?.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 1. What: Writes [plan] as the `workoutPlan` map on `users/{uid}`, merging so the
     *    profile/preferences fields are preserved; returns the outcome as a [Result].
     * 2. Who: The Firestore implementation of [WorkoutRepository.savePlan].
     * 3. When: After a plan is generated or edited, from a coroutine.
     */
    override suspend fun savePlan(plan: WorkoutPlan): Result<Unit> {
        return try {
            val uid = requireUid()
            firestore.collection(USERS).document(uid)
                .set(mapOf(WORKOUT_PLAN to plan.toDto()), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 1. What: Seeds the prompt from the user's preferences, generates a plan (AI, falling back to
     *    a mock), stamps it with the current time, persists it, and returns the stamped plan.
     * 2. Who: The Firestore + AI implementation of [WorkoutRepository.generateAndSavePlan].
     * 3. When: When there's no saved plan, or the user taps "regenerate", from a coroutine.
     */
    override suspend fun generateAndSavePlan(): Result<WorkoutPlan> {
        return try {
            // A new week replaces the current plan — archive the finished week first so History
            // keeps it. (New weeks only start once the prior week is over, so it's a full week.)
            getPlan().getOrNull()?.let { archiveWeek(it) }

            val today = LocalDate.now()
            val plan = generatePlan(today).getOrThrow()
            // Stamp with the current time so the UI can tell when a week has passed, then persist
            // whatever we ended up with (AI or mock) and return the stamped plan.
            val stamped = plan.copy(generatedAt = System.currentTimeMillis(), startDate = today)
            savePlan(stamped)
            Result.success(stamped)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 1. What: Generates a plan anchored at [startDate] (AI, with a mock fallback honoring the
     *    requested day count) but does NOT save it — the caller merges/persists.
     * 2. Who: The Firestore + AI implementation of [WorkoutRepository.generatePlan].
     * 3. When: When the user redoes the current week, from a coroutine.
     */
    override suspend fun generatePlan(startDate: LocalDate): Result<WorkoutPlan> {
        return try {
            // Pull the user's onboarding answers to seed the prompt; if they're missing, use
            // sensible defaults so we can still produce a plan.
            val prefs = onboardingRepository.getPreferences().getOrNull()
            val plan = if (prefs != null) {
                generator.generate(prefs, startDate).getOrElse {
                    MockWorkoutGenerator.generate(trainingDays = prefs.daysPerWeek, startDate = startDate)
                }
            } else {
                MockWorkoutGenerator.generate(startDate = startDate)
            }
            Result.success(plan)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 1. What: Asks the AI to adjust [day] in [direction] (same exercises, shifted sets/reps); on
     *    any failure — or when preferences are missing — falls back to a deterministic nudge so
     *    the call always yields a usable day. Does not persist; the ViewModel saves the spliced plan.
     * 2. Who: The Firestore + AI implementation of [WorkoutRepository.adjustDayDifficulty].
     * 3. When: When the user picks "Make it easier" / "Make it harder", from a coroutine.
     */
    override suspend fun adjustDayDifficulty(
        day: DayPlan,
        direction: DifficultyAdjustment,
    ): Result<DayPlan> {
        return try {
            val prefs = onboardingRepository.getPreferences().getOrNull()
            val adjusted = if (prefs != null) {
                generator.adjustDayDifficulty(prefs, day, direction)
                    .getOrElse { MockWorkoutGenerator.adjustDifficulty(day, direction) }
            } else {
                MockWorkoutGenerator.adjustDifficulty(day, direction)
            }
            Result.success(adjusted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 1. What: Reads every archived week from `users/{uid}/workoutHistory`, mapped to domain and
     *    sorted most-recent-first.
     * 2. Who: The Firestore implementation of [WorkoutRepository.getHistory].
     * 3. When: When the History tab loads, from a coroutine.
     */
    override suspend fun getHistory(): Result<List<WeekRecord>> {
        return try {
            val uid = requireUid()
            val snapshot = firestore.collection(USERS).document(uid)
                .collection(WORKOUT_HISTORY).get().await()
            val weeks = snapshot.toObjects(WeekRecordDto::class.java)
                .map { it.toDomain() }
                .sortedByDescending { it.startDate }
            Result.success(weeks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 1. What: Computes profile stats from the user's archived weeks plus the current plan: total
     *    fully-completed workout days (a day where every exercise was checked off), and the number
     *    of weeks in which every workout day was fully completed. Best-effort — missing data just
     *    counts as zero.
     * 2. Who: The Firestore implementation of [WorkoutRepository.getProfileStats].
     * 3. When: When the Account tab loads, from a coroutine.
     */
    override suspend fun getProfileStats(): Result<ProfileStats> {
        return try {
            val history = getHistory().getOrDefault(emptyList())
            val current = getPlan().getOrNull()

            val completedInHistory = history.sumOf { week ->
                week.days.count { it.status == DayStatus.COMPLETED }
            }
            val completedThisWeek = current?.days?.count { day ->
                !day.isRest && day.completedAt != null &&
                    day.exercises.isNotEmpty() && day.exercises.all { it.done }
            } ?: 0

            // A "perfect" week is one with workout days where every one was fully completed.
            val weekStreak = history.count { week ->
                week.days.isNotEmpty() && week.days.all { it.status == DayStatus.COMPLETED }
            }

            Result.success(ProfileStats(completedInHistory + completedThisWeek, weekStreak))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 1. What: Archives a finished [plan] as a document in `users/{uid}/workoutHistory`, keyed by
     *    the week's start epoch-day (idempotent — re-archiving the same week overwrites it).
     *    Best-effort: failures are swallowed so they never block starting a new week.
     * 2. Who: Used by [generateAndSavePlan] just before the current plan is overwritten.
     * 3. When: When the user starts a new week, from a coroutine.
     */
    private suspend fun archiveWeek(plan: WorkoutPlan) {
        runCatching {
            val uid = requireUid()
            val record = plan.toWeekRecord()
            firestore.collection(USERS).document(uid)
                .collection(WORKOUT_HISTORY).document(record.startDate.toEpochDay().toString())
                .set(record.toDto())
                .await()
        }
    }

    /**
     * 1. What: Returns the signed-in user's uid, throwing if no one is signed in.
     * 2. Who: Used by every Firestore read/write in this repository to scope to the user's document.
     * 3. When: At the start of each repository operation.
     */
    private fun requireUid(): String =
        firebaseAuth.currentUser?.uid ?: throw IllegalStateException("No signed-in user")

    // Stores all collections to be accessed throughout document
    companion object {
        private const val USERS = "users"
        private const val WORKOUT_PLAN = "workoutPlan"
        private const val WORKOUT_HISTORY = "workoutHistory"
    }
}
