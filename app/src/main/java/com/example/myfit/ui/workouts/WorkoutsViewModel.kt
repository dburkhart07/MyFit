package com.example.myfit.ui.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfit.data.WorkoutRepository
import com.example.myfit.data.WorkoutRepositoryImpl
import com.example.myfit.model.DayPlan
import com.example.myfit.model.DifficultyAdjustment
import com.example.myfit.model.RegenerateMode
import com.example.myfit.model.WorkoutPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * The states the Workouts tab can be in. [Generating] is distinct from [Loading] so the UI can
 * show "Building your plan…" (AI call) versus a quick Firestore read.
 */
sealed interface WorkoutsUiState {
    data object Loading : WorkoutsUiState
    data object Generating : WorkoutsUiState

    /**
     * The loaded plan. [adjustingDayIndex] is non-null while that day's difficulty is being
     * re-prompted, so the detail view can show a localized spinner and disable its buttons without
     * tearing down to the global Generating state.
     */
    data class Loaded(val plan: WorkoutPlan, val adjustingDayIndex: Int? = null) : WorkoutsUiState
    data class Error(val message: String) : WorkoutsUiState
}

/**
 * 1. What: Owns the Workouts tab's data flow so loads the saved plan, generates one via the
 *    repository (Gemini + mock fallback) when none exists, and persists edits (swap/delete/add).
 *    Exposes everything as [uiState]. Mirrors OnboardingViewModel.
 * 2. Who: Created by WorkoutsScreen via `viewModel()`.
 * 3. When: [load] runs when the screen appears; edits and regeneration run on user actions.
 */
class WorkoutsViewModel(
    private val repository: WorkoutRepository = WorkoutRepositoryImpl(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<WorkoutsUiState>(WorkoutsUiState.Loading)
    val uiState: StateFlow<WorkoutsUiState> = _uiState.asStateFlow()

    /**
     * 1. What: Loads the saved plan; if there isn't one, kicks off generation instead.
     * 2. Who: Called by WorkoutsScreen.
     * 3. When: When the screen first appears, and on retry after an error.
     */
    fun load() {
        _uiState.value = WorkoutsUiState.Loading
        viewModelScope.launch {
            repository.getPlan()
                .onSuccess { plan ->
                    if (plan != null) {
                        _uiState.value = WorkoutsUiState.Loaded(plan)
                    } else {
                        generate()
                    }
                }
                .onFailure {
                    _uiState.value = WorkoutsUiState.Error(it.message ?: "Could not load your plan")
                }
        }
    }

    /**
     * 1. What: Generates a brand-new week (AI, with mock fallback) starting today and saves it.
     * 2. Who: Called by [load] when no plan exists, and on retry after an error.
     * 3. When: First-time setup or an error retry.
     */
    fun generate() = generate(RegenerateMode.NEW_WEEK)

    /**
     * 1. What: Rebuilds the plan per [mode]. [RegenerateMode.NEW_WEEK] starts a fresh seven-day
     *    plan from today; [RegenerateMode.REDO_CURRENT_WEEK] re-prompts only the days that are
     *    still to come, keeping past and already-attempted days (and the week's start/stamp).
     * 2. Who: Called by the screen's regenerate modal and the stale-week dialog.
     * 3. When: When the user confirms a regenerate choice.
     */
    fun generate(mode: RegenerateMode) {
        when (mode) {
            RegenerateMode.NEW_WEEK -> {
                _uiState.value = WorkoutsUiState.Generating
                viewModelScope.launch {
                    repository.generateAndSavePlan()
                        .onSuccess { _uiState.value = WorkoutsUiState.Loaded(it) }
                        .onFailure {
                            _uiState.value = WorkoutsUiState.Error(it.message ?: "Could not generate a plan")
                        }
                }
            }

            RegenerateMode.REDO_CURRENT_WEEK -> {
                val current = (uiState.value as? WorkoutsUiState.Loaded)?.plan ?: return
                val indices = current.remainingDayIndices(LocalDate.now()).toSet()
                if (indices.isEmpty()) return
                _uiState.value = WorkoutsUiState.Generating
                viewModelScope.launch {
                    repository.generatePlan(current.startDate)
                        .onSuccess { fresh ->
                            // Replace only the remaining day-slots with freshly generated ones;
                            // keep past/attempted days (and the original start date + stamp).
                            val mergedDays = current.days.mapIndexed { i, day ->
                                if (i in indices) fresh.days[i] else day
                            }
                            val merged = current.copy(days = mergedDays)
                            _uiState.value = WorkoutsUiState.Loaded(merged)
                            launch { repository.savePlan(merged) }
                        }
                        .onFailure {
                            _uiState.value = WorkoutsUiState.Error(it.message ?: "Could not regenerate your week")
                        }
                }
            }
        }
    }

    /**
     * 1. What: Replaces the whole plan in memory and persists it (fire-and-forget save).
     * 2. Who: Called by the screen after swap/delete/add edits build a new plan.
     * 3. When: On each weekly-plan edit.
     */
    fun updatePlan(plan: WorkoutPlan) {
        _uiState.value = WorkoutsUiState.Loaded(plan)
        viewModelScope.launch { repository.savePlan(plan) }
    }

    /**
     * 1. What: Convenience edit — replaces the day at [index] with [newDay] and saves.
     * 2. Who: Called by the screen for add/delete/swap operations on a single day.
     * 3. When: When the user adds, deletes, or swaps a day's workout.
     */
    fun updateDay(index: Int, newDay: DayPlan) {
        val current = (uiState.value as? WorkoutsUiState.Loaded)?.plan ?: return
        val updatedDays = current.days.toMutableList().also { it[index] = newDay }
        updatePlan(current.copy(days = updatedDays))
    }

    /**
     * 1. What: Re-prompts the AI to make the day at [index] [direction] (same exercises, shifted
     *    sets/reps), then splices the result into the plan and persists it. Flags the day as
     *    adjusting while in flight so the detail view shows a spinner; the repository falls back to
     *    a deterministic nudge, so this effectively never fails.
     * 2. Who: Called by the screen when the user confirms "Make it easier" / "Make it harder".
     * 3. When: On that user action.
     */
    fun adjustDayDifficulty(index: Int, direction: DifficultyAdjustment) {
        val current = (uiState.value as? WorkoutsUiState.Loaded)?.plan ?: return
        _uiState.value = WorkoutsUiState.Loaded(current, adjustingDayIndex = index)
        viewModelScope.launch {
            repository.adjustDayDifficulty(current.days[index], direction)
                .onSuccess { newDay ->
                    val updatedDays = current.days.toMutableList().also { it[index] = newDay }
                    val newPlan = current.copy(days = updatedDays)
                    _uiState.value = WorkoutsUiState.Loaded(newPlan)
                    launch { repository.savePlan(newPlan) }
                }
                .onFailure { _uiState.value = WorkoutsUiState.Loaded(current) }
        }
    }

    /**
     * 1. What: Records a finished workout — stamps the day at [index] with `completedAt = now` and
     *    writes each exercise's [doneFlags] checkmark, then persists. This is what colors the day's
     *    card (all done → green, some → yellow, none → red) and excludes it from a current-week redo.
     * 2. Who: Called by the detail view when the user taps "Complete workout".
     * 3. When: On workout completion.
     */
    fun completeDay(index: Int, doneFlags: List<Boolean>) {
        val current = (uiState.value as? WorkoutsUiState.Loaded)?.plan ?: return
        val day = current.days[index]
        val updatedExercises = day.exercises.mapIndexed { i, ex ->
            ex.copy(done = doneFlags.getOrElse(i) { false })
        }
        val updatedDay = day.copy(exercises = updatedExercises, completedAt = System.currentTimeMillis())
        updateDay(index, updatedDay)
    }
}