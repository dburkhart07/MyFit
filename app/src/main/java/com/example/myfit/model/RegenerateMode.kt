package com.example.myfit.model

/**
 * 1. What: The two ways the user can rebuild their plan from the "Regenerate plan" modal.
 *    [REDO_CURRENT_WEEK] re-prompts only the days that are still to come this week (keeping past
 *    and already-attempted days); [NEW_WEEK] starts a brand-new seven-day plan from today.
 * 2. Who: Chosen in [com.example.myfit.ui.workouts.WorkoutsScreen], handled by WorkoutsViewModel.
 * 3. When: When the user confirms a choice in the regenerate dialog.
 */
enum class RegenerateMode { REDO_CURRENT_WEEK, NEW_WEEK }
