package com.example.myfit.ui.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfit.data.WorkoutRepository
import com.example.myfit.data.WorkoutRepositoryImpl
import com.example.myfit.model.DayPlan
import com.example.myfit.model.WorkoutPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The states the Workouts tab can be in. [Generating] is distinct from [Loading] so the UI can
 * show "Building your plan…" (AI call) versus a quick Firestore read.
 */
sealed interface WorkoutsUiState {
    data object Loading : WorkoutsUiState
    data object Generating : WorkoutsUiState
    data class Loaded(val plan: WorkoutPlan) : WorkoutsUiState
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
     * 1. What: Generates a fresh plan (AI, with mock fallback) and saves it.
     * 2. Who: Called by [load] when no plan exists, and by the screen's "regenerate" action.
     * 3. When: First-time setup or an explicit regenerate.
     */
    fun generate() {
        _uiState.value = WorkoutsUiState.Generating
        viewModelScope.launch {
            repository.generateAndSavePlan()
                .onSuccess { _uiState.value = WorkoutsUiState.Loaded(it) }
                .onFailure {
                    _uiState.value = WorkoutsUiState.Error(it.message ?: "Could not generate a plan")
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
        updatePlan(WorkoutPlan(updatedDays))
    }
}