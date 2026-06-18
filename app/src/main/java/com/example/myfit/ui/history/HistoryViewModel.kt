package com.example.myfit.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfit.data.WorkoutRepository
import com.example.myfit.data.WorkoutRepositoryImpl
import com.example.myfit.model.DayStatus
import com.example.myfit.model.WeekRecord
import com.example.myfit.ui.common.DailyWorkout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

/**
 * The states the History tab can be in. [Loaded] carries the archived weeks already shaped for the
 * UI (newest first); it may be empty when the user hasn't finished a week yet.
 */
sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data class Loaded(val weeks: List<WeekHistory>) : HistoryUiState
    data class Error(val message: String) : HistoryUiState
}

/**
 * 1. What: Owns the History tab's data — reads the user's archived weeks from the repository and
 *    maps them to the UI's [WeekHistory] / [DailyWorkout] models (most recent first).
 * 2. Who: Created by HistoryScreen via `viewModel()`.
 * 3. When: [load] runs when the screen appears.
 */
class HistoryViewModel(
    private val repository: WorkoutRepository = WorkoutRepositoryImpl(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    fun load() {
        _uiState.value = HistoryUiState.Loading
        viewModelScope.launch {
            repository.getHistory()
                .onSuccess { records -> _uiState.value = HistoryUiState.Loaded(records.map { it.toUi() }) }
                .onFailure {
                    _uiState.value = HistoryUiState.Error(it.message ?: "Could not load your history")
                }
        }
    }

    private fun WeekRecord.toUi(): WeekHistory = WeekHistory(
        week = "Week of ${startDate.format(WEEK_FORMAT)}",
        completed = days.count { it.status == DayStatus.COMPLETED },
        total = days.size,
        dailyWorkouts = days.map { DailyWorkout(it.day, it.focus, it.completed, it.total) },
    )

    companion object {
        private val WEEK_FORMAT = DateTimeFormatter.ofPattern("MMM d")
    }
}
