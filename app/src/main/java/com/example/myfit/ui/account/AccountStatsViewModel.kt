package com.example.myfit.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfit.data.WorkoutRepository
import com.example.myfit.data.WorkoutRepositoryImpl
import com.example.myfit.model.ProfileStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 1. What: Loads the Account profile stats (completed workouts + week streak) from the workout
 *    repository and exposes them as [stats]; null until the first load resolves.
 * 2. Who: Created by AccountScreen via `viewModel()`.
 * 3. When: [load] runs when the Account tab appears.
 */
class AccountStatsViewModel(
    private val repository: WorkoutRepository = WorkoutRepositoryImpl(),
) : ViewModel() {

    private val _stats = MutableStateFlow<ProfileStats?>(null)
    val stats: StateFlow<ProfileStats?> = _stats.asStateFlow()

    /**
     * 1. What: Fetches the profile stats and publishes them to [stats]; on failure leaves the
     *    previous value (the card simply keeps showing its placeholder).
     * 2. Who: Called by AccountScreen.
     * 3. When: When the Account tab appears.
     */
    fun load() {
        viewModelScope.launch {
            repository.getProfileStats().onSuccess { _stats.value = it }
        }
    }
}
