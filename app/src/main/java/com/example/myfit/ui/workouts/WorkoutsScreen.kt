package com.example.myfit.ui.workouts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myfit.data.MockWorkoutGenerator
import com.example.myfit.model.DayPlan
import com.example.myfit.model.DayStatus
import com.example.myfit.model.DifficultyAdjustment
import com.example.myfit.model.Exercise
import com.example.myfit.model.RegenerateMode
import com.example.myfit.model.WorkoutPlan
import com.example.myfit.ui.common.BottomTab
import com.example.myfit.ui.common.CompletionIcon
import com.example.myfit.ui.common.MyFitBottomBar
import com.example.myfit.ui.common.MyFitTopBar
import com.example.myfit.ui.theme.Green400
import com.example.myfit.ui.theme.MyFitTheme
import com.example.myfit.ui.theme.Yellow400
import java.time.LocalDate

/** The phases a single workout session moves through in the detail view. */
private enum class Phase { NotStarted, InProgress, Completed }

/**
 * Per-exercise state during a live session. The sets/reps targets are locked once the workout
 * begins; only [done] (the completion checkbox) is mutable.
 */
private class ExerciseRowState(
    val name: String,
    val sets: Int,
    val reps: Int,
    initialDone: Boolean = false,
) {
    var done by mutableStateOf(initialDone)
}

/**
 * 1. What: Workouts tab — loads the user's AI-generated weekly plan from the ViewModel and shows
 *          loading / generating / error / loaded states. Loaded shows the weekly plan; workout
 *          days open an in-screen detail (Begin → check off → Complete); rest days can add a
 *          workout; upcoming workout days can be swapped or deleted while past days lock. Cards are
 *          color-coded by completion, and a non-dismissible "new week" dialog blocks a stale plan.
 *          Edits persist via the ViewModel.
 * 2. Who: Called by the app's NavHost (the Workouts destination).
 * 3. When: Shown after login/onboarding and whenever the Workouts tab is selected.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreen(
    onLogout: () -> Unit,
    onSelectTab: (BottomTab) -> Unit,
    viewModel: WorkoutsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = { MyFitTopBar(onLogout = onLogout) },
        bottomBar = { MyFitBottomBar(current = BottomTab.Workouts, onSelect = onSelectTab) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (val state = uiState) {
            is WorkoutsUiState.Loading ->
                StatusBox(message = "Loading your plan…", modifier = modifier)

            is WorkoutsUiState.Generating ->
                StatusBox(message = "Building your plan…", modifier = modifier)

            is WorkoutsUiState.NeedsPlan -> {
                // First-time user: same "New week!" modal a returning user gets at week's end.
                Column(modifier = modifier.fillMaxSize()) {}
                NewWeekDialog(
                    message = "Welcome! Generate your first weekly plan to get started.",
                    onGenerate = { viewModel.generate(RegenerateMode.NEW_WEEK) },
                )
            }

            is WorkoutsUiState.Error ->
                ErrorBox(message = state.message, onRetry = { viewModel.load() }, modifier = modifier)

            is WorkoutsUiState.Loaded ->
                LoadedContent(
                    plan = state.plan,
                    adjustingDayIndex = state.adjustingDayIndex,
                    onUpdateDay = viewModel::updateDay,
                    onAdjustDifficulty = viewModel::adjustDayDifficulty,
                    onCompleteDay = viewModel::completeDay,
                    onGenerate = { mode -> viewModel.generate(mode) },
                    modifier = modifier,
                )
        }
    }
}

/**
 * 1. What: Stateful host for the loaded plan — owns selected-day, swap/delete dialog state, and
 *          dispatches edits up to the ViewModel.
 * 2. Who: Rendered by [WorkoutsScreen] in the Loaded state.
 * 3. When: Once a plan is available.
 */
@Composable
private fun LoadedContent(
    plan: WorkoutPlan,
    adjustingDayIndex: Int?,
    onUpdateDay: (Int, DayPlan) -> Unit,
    onAdjustDifficulty: (Int, DifficultyAdjustment) -> Unit,
    onCompleteDay: (Int, List<Boolean>) -> Unit,
    onGenerate: (RegenerateMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { LocalDate.now() }
    var selectedDay by remember { mutableStateOf<Int?>(null) }
    var swapForIndex by remember { mutableStateOf<Int?>(null) }
    var deleteForIndex by remember { mutableStateOf<Int?>(null) }
    var showRegenerateDialog by remember { mutableStateOf(false) }
    val days = plan.days

    val sel = selectedDay
    if (sel == null) {
        WeeklyPlanView(
            plan = plan,
            today = today,
            onOpen = { i -> if (!days[i].isRest) selectedDay = i },
            onSwap = { swapForIndex = it },
            onDelete = { deleteForIndex = it },
            onAdd = { i -> onUpdateDay(i, newWorkoutFor(days[i].day)) },
            onRegenerate = { showRegenerateDialog = true },
            modifier = modifier,
        )
    } else {
        WorkoutDetailView(
            dayPlan = days[sel],
            isPast = plan.dateOf(sel).isBefore(today),
            isAdjusting = adjustingDayIndex == sel,
            onAdjust = { direction -> onAdjustDifficulty(sel, direction) },
            onComplete = { flags -> onCompleteDay(sel, flags) },
            onBack = { selectedDay = null },
            modifier = modifier,
        )
    }

    // Swap dialog — trade this day's workout with another day. Past or already-completed days
    // can't be swapped with.
    swapForIndex?.let { i ->
        SwapDialog(
            sourceDay = days[i].day,
            options = days.indices
                .filter { it != i && !plan.dateOf(it).isBefore(today) && days[it].completedAt == null }
                .map { it to days[it] },
            onDismiss = { swapForIndex = null },
            onPick = { target ->
                val a = days[i]
                val b = days[target]
                onUpdateDay(i, a.copy(focus = b.focus, isRest = b.isRest, exercises = b.exercises))
                onUpdateDay(target, b.copy(focus = a.focus, isRest = a.isRest, exercises = a.exercises))
                swapForIndex = null
            },
        )
    }

    // Regenerate modal — mid-week the user can only redo the days they have left; a brand-new week
    // can only be started once this week is over (via the forced dialog below).
    if (showRegenerateDialog) {
        RegenerateDialog(
            canRedoCurrentWeek = plan.remainingDayIndices(today).isNotEmpty(),
            onDismiss = { showRegenerateDialog = false },
            onConfirm = {
                showRegenerateDialog = false
                onGenerate(RegenerateMode.REDO_CURRENT_WEEK)
            },
        )
    }

    // Week over — a hard block until the user starts a fresh week, keeping every week a full 7 days.
    if (plan.isWeekOver(today)) {
        NewWeekDialog(onGenerate = { onGenerate(RegenerateMode.NEW_WEEK) })
    }

    // Delete confirmation — turns the day into a rest day.
    deleteForIndex?.let { i ->
        val day = days[i]
        val colors = MaterialTheme.colorScheme
        AlertDialog(
            onDismissRequest = { deleteForIndex = null },
            containerColor = colors.surface,
            titleContentColor = colors.onSurface,
            textContentColor = colors.onSurfaceVariant,
            title = { Text("Delete workout?") },
            text = {
                Text("Are you sure you want to delete ${day.day}'s workout (${day.focus})? This will turn it into a rest day.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateDay(i, day.copy(focus = "Rest", isRest = true, exercises = emptyList()))
                        deleteForIndex = null
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.error),
                ) { Text("Delete", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { deleteForIndex = null }) { Text("Cancel") }
            },
        )
    }
}

/**
 * 1. What: Builds a default workout for a day that was previously a rest day (UI-side add).
 * 2. Who: Used by [LoadedContent] when the user taps "Add a workout".
 * 3. When: On the add action; the ViewModel then persists the change.
 */
private fun newWorkoutFor(day: String) = DayPlan(
    day = day,
    focus = "New workout",
    isRest = false,
    exercises = listOf(
        Exercise("Bench press", 3, 10),
        Exercise("Rows", 3, 12),
        Exercise("Shoulder press", 3, 10),
        Exercise("Bicep curls", 3, 12),
    ),
)

/**
 * 1. What: Centered status box with a spinner and a message (Loading / Generating states).
 * 2. Who: Rendered by [WorkoutsScreen].
 * 3. When: While the plan is being read from Firestore or generated by the AI.
 */
@Composable
private fun StatusBox(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 1. What: Error message plus a Retry action.
 * 2. Who: Rendered by [WorkoutsScreen] in the Error state.
 * 3. When: When loading or generating the plan fails.
 */
@Composable
private fun ErrorBox(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry, shape = RoundedCornerShape(8.dp)) {
            Text("Retry", fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * 1. What: The weekly list of day cards (color-coded by completion) plus a "Regenerate plan" button.
 * 2. Who: Rendered by [LoadedContent] when no day is selected.
 * 3. When: The default state of the Workouts tab once a plan is loaded.
 */
@Composable
private fun WeeklyPlanView(
    plan: WorkoutPlan,
    today: LocalDate,
    onOpen: (Int) -> Unit,
    onSwap: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onAdd: (Int) -> Unit,
    onRegenerate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "This week",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        itemsIndexed(plan.days, key = { _, day -> day.day }) { index, day ->
            val date = plan.dateOf(index)
            DayPlanCard(
                day = day,
                status = day.status(date, today),
                locked = date.isBefore(today) || day.completedAt != null,
                onOpen = { onOpen(index) },
                onSwap = { onSwap(index) },
                onDelete = { onDelete(index) },
                onAdd = { onAdd(index) },
            )
        }
        item {
            OutlinedButton(
                onClick = onRegenerate,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text("Regenerate plan", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** The border color a card gets for its [DayStatus] (PENDING keeps the neutral outline). */
@Composable
private fun borderColorFor(status: DayStatus): Color = when (status) {
    DayStatus.COMPLETED -> Green400
    DayStatus.PARTIAL -> Yellow400
    DayStatus.MISSED -> MaterialTheme.colorScheme.error
    DayStatus.PENDING -> MaterialTheme.colorScheme.outline
}

/**
 * 1. What: One day card — header (title + count + chevron) opens the detail; its border is
 *          color-coded by [status] (green done, yellow partial, red missed). Workout days show
 *          Swap/Delete and rest days show "Add a workout" — but only while the day is not [locked]
 *          (past or already-completed days are locked).
 * 2. Who: Rendered for each day in [WeeklyPlanView].
 * 3. When: Always; tapping a workout day's header opens its detail view.
 */
@Composable
private fun DayPlanCard(
    day: DayPlan,
    status: DayStatus,
    locked: Boolean,
    onOpen: () -> Unit,
    onSwap: () -> Unit,
    onDelete: () -> Unit,
    onAdd: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val borderColor = borderColorFor(status)
    val borderWidth = if (status == DayStatus.PENDING) 1.dp else 2.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceVariant)
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (day.isRest) Modifier else Modifier.clickable(onClick = onOpen)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = day.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                )
                if (!day.isRest) {
                    Text(
                        text = day.exerciseCountLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            if (!day.isRest) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open workout",
                    tint = colors.onSurfaceVariant,
                )
            }
        }

        // Locked days (past or completed) can't be changed: no swap / delete / add.
        if (!locked) {
            if (day.isRest) {
                OutlinedButton(
                    onClick = onAdd,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Add a workout")
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onSwap,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                    ) { Text("Swap") }
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                    ) { Text("Delete workout", color = colors.error) }
                }
            }
        }
    }
}

/**
 * 1. What: The "Regenerate plan" modal — mid-week the only choice is to redo the days the user has
 *          left this week (a brand-new week can only start once this one ends, so every week stays
 *          a full seven days). Confirm is disabled when no days remain.
 * 2. Who: Shown by [LoadedContent] when the user taps "Regenerate plan".
 * 3. When: On that tap; confirm reports [RegenerateMode.REDO_CURRENT_WEEK].
 */
@Composable
private fun RegenerateDialog(
    canRedoCurrentWeek: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.onSurface,
        textContentColor = colors.onSurfaceVariant,
        title = { Text("Redo current week") },
        text = {
            Text(
                if (canRedoCurrentWeek) {
                    "Generate fresh workouts for the days you have left this week. A whole new week starts automatically once this one ends."
                } else {
                    "No days left to regenerate — a new week starts automatically once this one ends."
                },
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = canRedoCurrentWeek,
                shape = RoundedCornerShape(8.dp),
            ) { Text("Regenerate", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * 1. What: A non-dismissible "New week!" dialog whose only action generates a fresh week — the user
 *          can't tap away or back out of it. [message] adapts the copy (week rollover vs first run).
 * 2. Who: Shown by [LoadedContent] when [WorkoutPlan.isWeekOver] is true, and by [WorkoutsScreen]
 *          in the [WorkoutsUiState.NeedsPlan] (first-time) state.
 * 3. When: Once the week has fully elapsed, or on first login before any plan exists.
 */
@Composable
private fun NewWeekDialog(
    onGenerate: () -> Unit,
    message: String = "Your plan is from last week. Generate a fresh plan to keep going.",
) {
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        containerColor = colors.surface,
        titleContentColor = colors.onSurface,
        textContentColor = colors.onSurfaceVariant,
        title = { Text("New week!") },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onGenerate, shape = RoundedCornerShape(8.dp)) {
                Text("Generate a new plan", fontWeight = FontWeight.SemiBold)
            }
        },
    )
}

/**
 * 1. What: Dialog listing the other days so the user can pick one to swap this day's workout with.
 * 2. Who: Shown by [LoadedContent].
 * 3. When: When the user taps "Swap" on a workout day; dismissed on pick or cancel.
 */
@Composable
private fun SwapDialog(
    sourceDay: String,
    options: List<Pair<Int, DayPlan>>,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.onSurface,
        title = { Text("Swap $sourceDay with…") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (index, day) ->
                    OutlinedButton(
                        onClick = { onPick(index) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("${day.day} · ${day.focus}")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * 1. What: The detail for one day's workout. Before "Begin workout" the user can make it
 *          easier/harder (AI); after "Begin workout" the sets/reps are locked and each exercise
 *          gets a completion checkbox, ending in a completion summary.
 * 2. Who: Rendered by [LoadedContent] when a workout day is selected.
 * 3. When: After tapping a workout day; "Complete workout" and the back button both return to
 *    the weekly list.
 */
@Composable
private fun WorkoutDetailView(
    dayPlan: DayPlan,
    isPast: Boolean,
    isAdjusting: Boolean,
    onAdjust: (DifficultyAdjustment) -> Unit,
    onComplete: (List<Boolean>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val isCompleted = dayPlan.completedAt != null
    // A completed or past day is locked to read-only — no begin / alter / editing.
    val readOnly = isCompleted || isPast
    var phase by remember(dayPlan.day) { mutableStateOf(Phase.NotStarted) }
    var showAlterDialog by remember(dayPlan.day) { mutableStateOf(false) }
    // Key on the exercises (not just the day label) so the rows rebuild with new targets after a
    // difficulty adjustment changes the day's sets/reps; seed the checkboxes from saved progress.
    val rows = remember(dayPlan.exercises) {
        dayPlan.exercises.map { ExerciseRowState(it.name, it.sets, it.reps, it.done) }
    }

    if (phase == Phase.Completed) {
        WorkoutCompletedView(
            done = rows.count { it.done },
            total = rows.size,
            onBack = onBack,
            modifier = modifier,
        )
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = colors.secondary,
                    )
                    Text(
                        text = "Back to this week",
                        fontWeight = FontWeight.SemiBold,
                        color = colors.secondary,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = dayPlan.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                    )
                    Text(
                        text = "${rows.size} exercises",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    when {
                        isCompleted -> StatusNote(
                            text = "Completed — ${rows.count { it.done }} of ${rows.size} checked off",
                            color = Green400,
                        )
                        isPast -> StatusNote(text = "Missed — this day has passed", color = colors.error)
                    }
                }
            }
            items(rows) { row ->
                ExerciseRow(
                    row = row,
                    showCheckbox = phase == Phase.InProgress || isCompleted,
                    checkboxEnabled = phase == Phase.InProgress,
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when {
                        readOnly -> Unit  // locked: review/missed has no actions
                        phase == Phase.NotStarted -> {
                            if (isAdjusting) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = "Adjusting your workout…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.onSurfaceVariant,
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { showAlterDialog = true },
                                enabled = !isAdjusting,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Alter daily workout") }
                            Button(
                                onClick = { phase = Phase.InProgress },
                                enabled = !isAdjusting,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                            ) { Text("Begin workout", fontWeight = FontWeight.SemiBold) }
                        }
                        phase == Phase.InProgress -> {
                            Button(
                                onClick = {
                                    onComplete(rows.map { it.done })
                                    phase = Phase.Completed
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                            ) { Text("Complete workout", fontWeight = FontWeight.SemiBold) }
                            TextButton(
                                onClick = {
                                    rows.forEach { it.done = false }
                                    phase = Phase.NotStarted
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Stop workout", color = colors.error) }
                        }
                    }
                }
            }
        }
    }

    if (showAlterDialog) {
        AlterWorkoutDialog(
            workoutTitle = dayPlan.title,
            onDismiss = { showAlterDialog = false },
            onConfirm = { direction ->
                onAdjust(direction)
                showAlterDialog = false
            },
        )
    }
}

/** A small colored status line under the workout title (e.g. "Completed", "Missed"). */
@Composable
private fun StatusNote(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
    )
}

/**
 * 1. What: One exercise row — name and its (locked) sets×reps target. A completion checkbox shows
 *          when [showCheckbox] is true; it's interactive only when [checkboxEnabled] (a live
 *          session), and shown checked-but-disabled when reviewing a completed workout.
 * 2. Who: Rendered for each exercise in [WorkoutDetailView].
 * 3. When: The checkbox appears during a live session or when reviewing a completed day.
 */
@Composable
private fun ExerciseRow(row: ExerciseRowState, showCheckbox: Boolean, checkboxEnabled: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceVariant)
            .border(1.dp, colors.outline, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showCheckbox) {
            Checkbox(
                checked = row.done,
                onCheckedChange = if (checkboxEnabled) ({ row.done = it }) else null,
                enabled = checkboxEnabled,
            )
            Spacer(Modifier.width(8.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = row.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
            )
            Text(
                text = "${row.sets} × ${row.reps}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

/**
 * 1. What: The completion summary — gradient check, "Workout Completed!", count, and a button back.
 * 2. Who: Rendered by [WorkoutDetailView] once the phase becomes Completed.
 * 3. When: After the user taps "Complete workout"; the button returns to the weekly list.
 */
@Composable
private fun WorkoutCompletedView(
    done: Int,
    total: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CompletionIcon(completed = true)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Workout Completed!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "$done of $total exercises checked off",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onBack,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text("Back to workouts", fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * 1. What: Dialog offering ways to change today's workout — make it easier or harder. (Swapping a
 *          whole day lives on the weekly "This week" view, not here.)
 * 2. Who: Shown by [WorkoutDetailView].
 * 3. When: When the user taps "Alter daily workout"; confirm reports the chosen [DifficultyAdjustment].
 */
@Composable
private fun AlterWorkoutDialog(
    workoutTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (DifficultyAdjustment) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val options = listOf(
        "Make it easier" to DifficultyAdjustment.EASIER,
        "Make it harder" to DifficultyAdjustment.HARDER,
    )
    var selected by remember { mutableStateOf(DifficultyAdjustment.HARDER) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.onSurface,
        textContentColor = colors.onSurfaceVariant,
        title = { Text(workoutTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("How do you want to change this workout?", style = MaterialTheme.typography.bodyMedium)
                options.forEach { (label, direction) ->
                    val isSelected = direction == selected
                    Surface(
                        onClick = { selected = direction },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) colors.surfaceContainerHighest else colors.surfaceVariant,
                        contentColor = if (isSelected) colors.onSurface else colors.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selected) }, shape = RoundedCornerShape(8.dp)) {
                Text("Confirm", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/* ----------------------------- Previews ----------------------------- */
// Previews use MockWorkoutGenerator (pure Kotlin, no network/ViewModel) so they render cleanly.

/**
 * 1. What: Design-time preview of the loaded weekly plan.
 * 2. Who: Called by Android Studio's Compose preview renderer.
 * 3. When: Rendered at design time in the IDE; never runs in the shipped app.
 */
@Preview(showBackground = true)
@Composable
private fun LoadedContentPreview() {
    MyFitTheme {
        LoadedContent(
            plan = MockWorkoutGenerator.generate(trainingDays = 4),
            adjustingDayIndex = null,
            onUpdateDay = { _, _ -> },
            onAdjustDifficulty = { _, _ -> },
            onCompleteDay = { _, _ -> },
            onGenerate = {},
        )
    }
}

/**
 * 1. What: Design-time preview of the weekly plan with the non-dismissible stale "new week" dialog.
 * 2. Who: Called by Android Studio's Compose preview renderer.
 * 3. When: Rendered at design time in the IDE; never runs in the shipped app.
 */
@Preview(showBackground = true)
@Composable
private fun StaleWeeklyPlanPreview() {
    MyFitTheme {
        LoadedContent(
            plan = MockWorkoutGenerator.generate(trainingDays = 4).copy(generatedAt = 1L),
            adjustingDayIndex = null,
            onUpdateDay = { _, _ -> },
            onAdjustDifficulty = { _, _ -> },
            onCompleteDay = { _, _ -> },
            onGenerate = {},
        )
    }
}

/**
 * 1. What: Design-time preview of a single workout-day card.
 * 2. Who: Called by Android Studio's Compose preview renderer.
 * 3. When: Rendered at design time in the IDE; never runs in the shipped app.
 */
@Preview(showBackground = true)
@Composable
private fun DayPlanCardPreview() {
    MyFitTheme {
        DayPlanCard(
            day = MockWorkoutGenerator.generate().days.first { !it.isRest },
            status = DayStatus.PENDING,
            locked = false,
            onOpen = {}, onSwap = {}, onDelete = {}, onAdd = {},
        )
    }
}

/**
 * 1. What: Design-time preview of the workout detail view (read-only / not-started state).
 * 2. Who: Called by Android Studio's Compose preview renderer.
 * 3. When: Rendered at design time in the IDE; never runs in the shipped app.
 */
@Preview(showBackground = true)
@Composable
private fun WorkoutDetailViewPreview() {
    MyFitTheme {
        WorkoutDetailView(
            dayPlan = MockWorkoutGenerator.generate().days.first { !it.isRest },
            isPast = false,
            isAdjusting = false,
            onAdjust = {},
            onComplete = {},
            onBack = {},
        )
    }
}

/**
 * 1. What: Design-time preview of a single exercise row with the in-progress checkbox showing.
 * 2. Who: Called by Android Studio's Compose preview renderer.
 * 3. When: Rendered at design time in the IDE; never runs in the shipped app.
 */
@Preview(showBackground = true)
@Composable
private fun ExerciseRowPreview() {
    MyFitTheme {
        ExerciseRow(row = ExerciseRowState("Bench press", 3, 10), showCheckbox = true)
    }
}

/**
 * 1. What: Design-time preview of the workout-completed summary.
 * 2. Who: Called by Android Studio's Compose preview renderer.
 * 3. When: Rendered at design time in the IDE; never runs in the shipped app.
 */
@Preview(showBackground = true)
@Composable
private fun WorkoutCompletedViewPreview() {
    MyFitTheme {
        WorkoutCompletedView(done = 3, total = 5, onBack = {})
    }
}

/**
 * 1. What: Design-time preview of the alter-workout dialog.
 * 2. Who: Called by Android Studio's Compose preview renderer.
 * 3. When: Rendered at design time in the IDE; never runs in the shipped app.
 */
@Preview(showBackground = true)
@Composable
private fun AlterWorkoutDialogPreview() {
    MyFitTheme {
        AlterWorkoutDialog(workoutTitle = "Mon · Upper body", onDismiss = {}, onConfirm = {})
    }
}

/**
 * 1. What: Design-time preview of the centered loading/status box.
 * 2. Who: Called by Android Studio's Compose preview renderer.
 * 3. When: Rendered at design time in the IDE; never runs in the shipped app.
 */
@Preview(showBackground = true)
@Composable
private fun StatusBoxPreview() {
    MyFitTheme { StatusBox(message = "Building your plan…") }
}

/**
 * 1. What: Design-time preview of the error box with a retry action.
 * 2. Who: Called by Android Studio's Compose preview renderer.
 * 3. When: Rendered at design time in the IDE; never runs in the shipped app.
 */
@Preview(showBackground = true)
@Composable
private fun ErrorBoxPreview() {
    MyFitTheme { ErrorBox(message = "Could not load your plan", onRetry = {}) }
}

/**
 * 1. What: Design-time preview of the weekly plan list on its own.
 * 2. Who: Called by Android Studio's Compose preview renderer.
 * 3. When: Rendered at design time in the IDE; never runs in the shipped app.
 */
@Preview(showBackground = true)
@Composable
private fun WeeklyPlanViewPreview() {
    MyFitTheme {
        WeeklyPlanView(
            plan = MockWorkoutGenerator.generate(trainingDays = 4),
            today = LocalDate.now(),
            onOpen = {}, onSwap = {}, onDelete = {}, onAdd = {}, onRegenerate = {},
        )
    }
}

/**
 * 1. What: Design-time preview of the mid-week "Redo current week" modal.
 * 2. Who: Called by Android Studio's Compose preview renderer.
 * 3. When: Rendered at design time in the IDE; never runs in the shipped app.
 */
@Preview(showBackground = true)
@Composable
private fun RegenerateDialogPreview() {
    MyFitTheme {
        RegenerateDialog(canRedoCurrentWeek = true, onDismiss = {}, onConfirm = {})
    }
}

/**
 * 1. What: Design-time preview of the non-dismissible end-of-week "New week!" dialog.
 * 2. Who: Called by Android Studio's Compose preview renderer.
 * 3. When: Rendered at design time in the IDE; never runs in the shipped app.
 */
@Preview(showBackground = true)
@Composable
private fun NewWeekDialogPreview() {
    MyFitTheme { NewWeekDialog(onGenerate = {}) }
}

/**
 * 1. What: Design-time preview of the swap-day picker dialog.
 * 2. Who: Called by Android Studio's Compose preview renderer.
 * 3. When: Rendered at design time in the IDE; never runs in the shipped app.
 */
@Preview(showBackground = true)
@Composable
private fun SwapDialogPreview() {
    MyFitTheme {
        val days = MockWorkoutGenerator.generate(trainingDays = 4).days
        SwapDialog(
            sourceDay = days.first().day,
            options = days.drop(1).mapIndexed { i, d -> (i + 1) to d },
            onDismiss = {},
            onPick = {},
        )
    }
}

/**
 * 1. What: Design-time preview of the colored status note under a workout title.
 * 2. Who: Called by Android Studio's Compose preview renderer.
 * 3. When: Rendered at design time in the IDE; never runs in the shipped app.
 */
@Preview(showBackground = true)
@Composable
private fun StatusNotePreview() {
    MyFitTheme { StatusNote(text = "Completed — 5 of 5 checked off", color = Green400) }
}