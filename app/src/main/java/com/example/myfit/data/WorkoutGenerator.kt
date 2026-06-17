package com.example.myfit.data

import com.example.myfit.model.DayPlan
import com.example.myfit.model.DifficultyAdjustment
import com.example.myfit.model.Exercise
import com.example.myfit.model.OnboardingPreferences
import com.example.myfit.model.WorkoutPlan
import com.example.myfit.model.dto.DayPlanDto
import com.example.myfit.model.dto.WorkoutPlanDto
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.LocalDate

/**
 * 1. What: The contract for generating a weekly [WorkoutPlan] from a user's preferences (and,
 *    later, their recent history). Mirrors the repository-interface style of the onboarding layer.
 * 2. Who: Implemented by [GeminiWorkoutGenerator]; consumed by [WorkoutRepositoryImpl].
 * 3. When: Called when the user has no saved plan, or taps "regenerate".
 */
interface WorkoutGenerator {
    /**
     * 1. What: Produces a 7-day plan tailored to [prefs], beginning on [startDate] (so day labels
     *    rotate to start on that weekday); [recentHistory] is optional extra context.
     * 2. Who: Implemented by [GeminiWorkoutGenerator]; called by the repository.
     * 3. When: On first load with no stored plan, or an explicit regenerate.
     */
    suspend fun generate(
        prefs: OnboardingPreferences,
        startDate: LocalDate,
        recentHistory: List<String> = emptyList(),
    ): Result<WorkoutPlan>

    /**
     * 1. What: Re-prompts the model to make a single [day]'s workout [direction] (easier/harder)
     *    while keeping the exact same exercises — only sets/reps change. Returns the adjusted day.
     * 2. Who: Implemented by [GeminiWorkoutGenerator]; called by the repository when the user picks
     *    "Make it easier" / "Make it harder" in the detail view.
     * 3. When: On that user action; a failed [Result] lets the repository fall back to a mock.
     */
    suspend fun adjustDayDifficulty(
        prefs: OnboardingPreferences,
        day: DayPlan,
        direction: DifficultyAdjustment,
    ): Result<DayPlan>
}

/**
 * Firebase AI Logic (Gemini) implementation. Prompts the model for STRICT JSON matching the
 * [WorkoutPlanDto] shape, parses it, and validates it through the same domain mapper used for
 * Firestore reads. Any failure (network, blocked content, malformed JSON, invalid plan) surfaces
 * as a failed [Result] so the repository can fall back to a mock — the UI never gets stuck.
 *
 * Uses the non-deprecated `com.google.firebase:firebase-ai` SDK (Firebase AI Logic).
 */
class GeminiWorkoutGenerator(
    private val model: GenerativeModel = Firebase.ai(backend = GenerativeBackend.googleAI())
        .generativeModel(MODEL_NAME),
    private val json: Json = Json { ignoreUnknownKeys = true },
) : WorkoutGenerator {

    override suspend fun generate(
        prefs: OnboardingPreferences,
        startDate: LocalDate,
        recentHistory: List<String>,
    ): Result<WorkoutPlan> = withContext(Dispatchers.IO) {
        runCatching {
            val labels = WorkoutPlan.orderedLabels(startDate)
            val response = model.generateContent(buildPrompt(prefs, labels, recentHistory))
            val raw = response.text ?: error("Empty AI response")
            android.util.Log.d("WorkoutAI", "RAW GEMINI RESPONSE:\n$raw")
            val plan = parsePlan(raw) ?: error("AI returned an invalid plan")
            // Pin the calendar anchor and force the day labels by position so AI drift can't
            // corrupt the week's dates/ordering.
            plan.copy(
                startDate = startDate,
                days = plan.days.mapIndexed { i, d -> d.copy(day = labels[i]) },
            )
        }.onFailure {
            android.util.Log.e("WorkoutAI", "Gemini generation failed", it)
        }
    }

    override suspend fun adjustDayDifficulty(
        prefs: OnboardingPreferences,
        day: DayPlan,
        direction: DifficultyAdjustment,
    ): Result<DayPlan> = withContext(Dispatchers.IO) {
        runCatching {
            val response = model.generateContent(buildAdjustPrompt(prefs, day, direction))
            val raw = response.text ?: error("Empty AI response")
            android.util.Log.d("WorkoutAI", "RAW GEMINI ADJUST RESPONSE:\n$raw")
            parseDay(raw, original = day) ?: error("AI returned an invalid adjusted day")
        }.onFailure {
            android.util.Log.e("WorkoutAI", "Gemini difficulty adjustment failed", it)
        }
    }

    /**
     * 1. What: Extracts the JSON object from the model's text (stripping any ```json fences or
     *    stray prose) and maps it through the strict domain validator; null if it can't.
     * 2. Who: Used by [generate].
     * 3. When: Immediately after the model responds.
     */
    private fun parsePlan(raw: String): WorkoutPlan? {
        // Be defensive: models sometimes wrap JSON in ```json ... ``` or add a sentence.
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start == -1 || end == -1 || end < start) return null
        val jsonText = raw.substring(start, end + 1)
        val dto = runCatching { json.decodeFromString<WorkoutPlanDto>(jsonText) }.getOrNull() ?: return null
        return dto.toDomain()
    }

    /**
     * 1. What: Parses a single adjusted day from the model's text and grafts its exercises onto
     *    [original], preserving the day's label and focus. Returns null (so the caller can fall
     *    back) if the JSON is invalid, the day fails domain validation, or the model changed the
     *    set of exercises — we only allow sets/reps to move.
     * 2. Who: Used by [adjustDayDifficulty].
     * 3. When: Immediately after the model responds to an adjust prompt.
     */
    private fun parseDay(raw: String, original: DayPlan): DayPlan? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start == -1 || end == -1 || end < start) return null
        val jsonText = raw.substring(start, end + 1)
        val dto = runCatching { json.decodeFromString<DayPlanDto>(jsonText) }.getOrNull() ?: return null
        val parsed = dto.toDomain() ?: return null
        // Guard: the model must keep the same exercises (same names, same order). Otherwise reject
        // so the deterministic fallback runs instead of silently swapping the workout.
        val sameExercises = parsed.exercises.size == original.exercises.size &&
            parsed.exercises.zip(original.exercises).all { (new, old) -> new.name == old.name }
        if (!sameExercises) return null
        return original.copy(exercises = parsed.exercises)
    }

    /**
     * 1. What: Builds the prompt: role, the user's preferences, optional recent history, and a
     *    rigid OUTPUT FORMAT spec so the response is machine-parseable.
     * 2. Who: Used by [generate].
     * 3. When: Before each generation call.
     *
     * The format block is the contract: exact day labels, the DTO field names, and "JSON only,
     * no markdown" — this is what lets [parsePlan] stay simple and trustworthy.
     */
    private fun buildPrompt(
        prefs: OnboardingPreferences,
        labels: List<String>,
        recentHistory: List<String>,
    ): String {
        val equipmentList = prefs.equipment.joinToString(", ") { it.label }
        val historyBlock = if (recentHistory.isEmpty()) {
            "No recent workout history is available."
        } else {
            "Recent workouts (most recent first):\n" + recentHistory.joinToString("\n") { "- $it" }
        }
        val labelList = labels.joinToString(",") { "\"$it\"" }
        val firstLabel = labels.first()

        return """
            You are a certified strength & conditioning coach generating a 7-day workout plan
            for a fitness app. The week starts on $firstLabel.

            User profile:
            - Goal: ${prefs.goal.label}
            - Training days per week: ${prefs.daysPerWeek}
            - Available equipment: $equipmentList
            - Experience level: ${prefs.experience.label}

            $historyBlock

            Design a balanced week. Make exactly ${prefs.daysPerWeek} of the 7 days workout days
            and the remaining days rest days, spread sensibly (avoid stacking all rest days
            together). Choose exercises that fit the available equipment and experience level.
            Each workout day should have 4-7 exercises.

            OUTPUT FORMAT — respond with RAW JSON ONLY. No markdown, no backticks, no commentary
            before or after. The JSON must match this exact shape:

            {
              "days": [
                {
                  "day": "$firstLabel",
                  "focus": "Upper body",
                  "isRest": false,
                  "exercises": [
                    { "name": "Bench press", "sets": 3, "reps": 10 }
                  ]
                }
              ]
            }

            Rules for the JSON:
            - "days" MUST contain exactly 7 objects, one per day.
            - "day" MUST use exactly these labels, in this order: $labelList.
            - For a rest day: set "isRest": true, "focus": "Rest", and "exercises": [].
            - For a workout day: "isRest": false, a short "focus" label, and a non-empty "exercises".
            - "sets" and "reps" are positive integers.
            - Output nothing except the JSON object.
        """.trimIndent()
    }

    /**
     * 1. What: Builds the single-day adjustment prompt — the user's profile, the day's current
     *    exercises, and a strict instruction to keep the same exercises while moving sets/reps to
     *    make the session [direction]. Outputs one day in the [DayPlanDto] shape.
     * 2. Who: Used by [adjustDayDifficulty].
     * 3. When: Before each adjustment call.
     */
    private fun buildAdjustPrompt(
        prefs: OnboardingPreferences,
        day: DayPlan,
        direction: DifficultyAdjustment,
    ): String {
        val exerciseLines = day.exercises.joinToString("\n") { "- ${it.name}: ${it.sets} sets × ${it.reps} reps" }

        return """
            You are a certified strength & conditioning coach adjusting one day of a workout plan
            for a fitness app.

            User profile:
            - Goal: ${prefs.goal.label}
            - Experience level: ${prefs.experience.label}

            Current workout for ${day.day} (focus: ${day.focus}):
            $exerciseLines

            Make this workout ${direction.label}. Keep the EXACT same exercises, in the same order —
            do NOT add, remove, rename, or reorder any exercise. Only change "sets" and/or "reps"
            so the overall session becomes ${direction.label} while staying safe and sensible for
            this experience level. Keep "sets" in 1..${Exercise.MAX_SETS} and "reps" in 1..${Exercise.MAX_REPS}.

            OUTPUT FORMAT — respond with RAW JSON ONLY. No markdown, no backticks, no commentary
            before or after. The JSON must match this exact shape:

            {
              "day": "${day.day}",
              "focus": "${day.focus}",
              "isRest": false,
              "exercises": [
                { "name": "Bench press", "sets": 3, "reps": 10 }
              ]
            }

            Rules for the JSON:
            - "day" MUST be "${day.day}" and "focus" MUST be "${day.focus}".
            - "isRest" MUST be false.
            - "exercises" MUST list the same exercises by name, in the same order as above.
            - "sets" and "reps" are positive integers.
            - Output nothing except the JSON object.
        """.trimIndent()
    }

    companion object {
        // Fast + cheap; swap for "gemini-2.5-pro" for higher-quality plans at higher cost.
        const val MODEL_NAME = "gemini-2.5-flash"
    }
}

/**
 * Deterministic offline fallback used when the AI call fails or is unavailable. Produces a sane
 * plan honoring the requested number of training days so the user always sees something.
 *
 * This is plain Kotlin (no network), so it doubles as the data source for previews and tests.
 */
object MockWorkoutGenerator {

    private val upperBody = listOf(
        Exercise("Bench press", 3, 10),
        Exercise("Rows", 3, 12),
        Exercise("Shoulder press", 3, 10),
        Exercise("Bicep curls", 3, 12),
        Exercise("Tricep dips", 3, 10),
    )
    private val lowerBody = listOf(
        Exercise("Squats", 4, 10),
        Exercise("Romanian deadlift", 3, 10),
        Exercise("Leg press", 3, 12),
        Exercise("Calf raises", 4, 15),
        Exercise("Lunges", 3, 12),
    )
    private val fullBody = listOf(
        Exercise("Deadlift", 3, 8),
        Exercise("Pull-ups", 3, 8),
        Exercise("Overhead press", 3, 10),
        Exercise("Goblet squat", 3, 12),
        Exercise("Push-ups", 3, 15),
    )

    private val rotation = listOf("Upper body" to upperBody, "Lower body" to lowerBody, "Full body" to fullBody)

    /**
     * 1. What: Builds a 7-day [WorkoutPlan] beginning on [startDate] with [trainingDays] workout
     *    days evenly spaced across the week and the rest as rest days. Day labels rotate to start
     *    on [startDate]'s weekday.
     * 2. Who: Called by [WorkoutRepositoryImpl] as the fallback, and by previews/tests.
     * 3. When: When the AI generation fails, or no generator is available.
     */
    fun generate(trainingDays: Int = 4, startDate: LocalDate = LocalDate.now()): WorkoutPlan {
        val days = WorkoutPlan.DAYS_IN_WEEK
        val target = trainingDays.coerceIn(1, days)
        // Evenly pick which day-indices are workout days.
        val workoutIndices = (0 until target).map { (it * days) / target }.toSet()
        val labels = WorkoutPlan.orderedLabels(startDate)

        var rotationIdx = 0
        val plan = labels.mapIndexed { index, label ->
            if (index in workoutIndices) {
                val (focus, exercises) = rotation[rotationIdx % rotation.size]
                rotationIdx++
                DayPlan(day = label, focus = focus, isRest = false, exercises = exercises)
            } else {
                DayPlan(day = label, focus = "Rest", isRest = true)
            }
        }
        return WorkoutPlan(days = plan, startDate = startDate)
    }

    /**
     * 1. What: Deterministic offline difficulty nudge — keeps the same exercises and shifts reps
     *    by ±2 (clamped to valid bounds) so a day always gets [direction] even without the AI.
     * 2. Who: Used by [WorkoutRepositoryImpl] as the fallback when the AI adjustment fails, and by
     *    previews/tests.
     * 3. When: When [GeminiWorkoutGenerator.adjustDayDifficulty] fails or no generator is available.
     */
    fun adjustDifficulty(day: DayPlan, direction: DifficultyAdjustment): DayPlan {
        val delta = if (direction == DifficultyAdjustment.HARDER) 2 else -2
        val adjusted = day.exercises.map { ex ->
            ex.copy(reps = (ex.reps + delta).coerceIn(1, Exercise.MAX_REPS))
        }
        return day.copy(exercises = adjusted)
    }
}