package com.example.myfit.data

import com.example.myfit.model.OnboardingPreferences
import com.example.myfit.model.WorkoutPlan
import com.example.myfit.model.dto.WorkoutPlanDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

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
     * 1. What: Generates a fresh plan from the user's preferences (falling back to a mock if the
     *    AI call fails), saves it, and returns it.
     * 2. Who: Implemented by [WorkoutRepositoryImpl]; called by WorkoutsViewModel.
     * 3. When: When there's no saved plan, or the user taps "regenerate".
     */
    suspend fun generateAndSavePlan(): Result<WorkoutPlan>
}

/**
 * Firestore-backed [WorkoutRepository]. Stores the plan as a `workoutPlan` map field on the
 * signed-in user's document (`users/{uid}`), using a merge write so it never clobbers the
 * profile/preferences fields written elsewhere — exactly the pattern in [OnboardingRepositoryImpl].
 *
 * Generation delegates to a [WorkoutGenerator] (Gemini) and falls back to [MockWorkoutGenerator]
 * if that fails, so the user always ends up with a usable plan.
 */
class WorkoutRepositoryImpl(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val generator: WorkoutGenerator = GeminiWorkoutGenerator(),
    private val onboardingRepository: OnboardingRepository = OnboardingRepositoryImpl(),
) : WorkoutRepository {

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

    override suspend fun generateAndSavePlan(): Result<WorkoutPlan> {
        return try {
            // Pull the user's onboarding answers to seed the prompt; if they're missing, use
            // sensible defaults so we can still produce a plan.
            val prefs = onboardingRepository.getPreferences().getOrNull()

            val plan = if (prefs != null) {
                // Try the AI; on any failure fall back to a mock honoring the requested day count.
                generator.generate(prefs).getOrElse {
                    MockWorkoutGenerator.generate(trainingDays = prefs.daysPerWeek)
                }
            } else {
                MockWorkoutGenerator.generate()
            }

            // Persist whatever we ended up with (AI or mock) and return it.
            savePlan(plan)
            Result.success(plan)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun requireUid(): String =
        firebaseAuth.currentUser?.uid ?: throw IllegalStateException("No signed-in user")

    companion object {
        private const val USERS = "users"
        private const val WORKOUT_PLAN = "workoutPlan"
    }
}