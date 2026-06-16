package com.example.myfit.ui.workouts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myfit.data.MockWorkoutGenerator
import com.example.myfit.model.DayPlan
import com.example.myfit.model.Exercise
import com.example.myfit.model.WorkoutPlan
import com.example.myfit.ui.components.BottomTab
import com.example.myfit.ui.components.CompletionIcon
import com.example.myfit.ui.components.MyFitBottomBar
import com.example.myfit.ui.components.MyFitTopBar
import com.example.myfit.ui.theme.MyFitTheme

/** The phases a single workout session moves through in the detail view. */
private enum class Phase { NotStarted, InProgress, Completed }

/**
 * Per-exercise editable state during a live session. Goal values stay fixed; sets/reps are
 * editable strings (simpler for text fields) and [done] backs the checkbox.
 */
private class ExerciseRowState(
    val name: String,
    val goalSets: Int,
    val goalReps: Int,
) {
    var sets by mutableStateOf(goalSets.toString())
    var reps by mutableStateOf(goalReps.toString())
    var done by mutableStateOf(false)
}

/**
 * 1. What: Workouts tab — loads the user's AI-generated weekly plan from the ViewModel and shows
 *          loading / generating / error / loaded states. Loaded shows the weekly plan; workout
 *          days open an in-screen detail (Begin → check off → Complete); rest days can add a
 *          workout; workout days can be swapped or deleted. Edits persist via the ViewModel.
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

            is WorkoutsUiState.Error ->
                ErrorBox(message = state.message, onRetry = { viewModel.load() }, modifier = modifier)

            is WorkoutsUiState.Loaded ->
                LoadedContent(
                    plan = state.plan,
                    onUpdateDay = viewModel::updateDay,
                    onRegenerate = viewModel::generate,
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
    onUpdateDay: (Int, DayPlan) -> Unit,
    onRegenerate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedDay by remember { mutableStateOf<Int?>(null) }
    var swapForIndex by remember { mutableStateOf<Int?>(null) }
    var deleteForIndex by remember { mutableStateOf<Int?>(null) }
    val days = plan.days

    val sel = selectedDay
    if (sel == null) {
        WeeklyPlanView(
            week = days,
            onOpen = { i -> if (!days[i].isRest) selectedDay = i },
            onSwap = { swapForIndex = it },
            onDelete = { deleteForIndex = it },
            onAdd = { i -> onUpdateDay(i, newWorkoutFor(days[i].day)) },
            onRegenerate = onRegenerate,
            modifier = modifier,
        )
    } else {
        WorkoutDetailView(
            dayPlan = days[sel],
            onBack = { selectedDay = null },
            modifier = modifier,
        )
    }

    // Swap dialog — trade this day's workout with another day.
    swapForIndex?.let { i ->
        SwapDialog(
            sourceDay = days[i].day,
            options = days.indices.filter { it != i }.map { it to days[it] },
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
 * 1. What: The weekly list of day cards plus a "Regenerate plan" button.
 * 2. Who: Rendered by [LoadedContent] when no day is selected.
 * 3. When: The default state of the Workouts tab once a plan is loaded.
 */
@Composable
private fun WeeklyPlanView(
    week: List<DayPlan>,
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
        itemsIndexed(week, key = { _, day -> day.day }) { index, day ->
            DayPlanCard(
                day = day,
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

/**
 * 1. What: One day card — header (title + count + chevron) opens the detail; workout days show
 *          Swap/Delete, rest days show "Add a workout".
 * 2. Who: Rendered for each day in [WeeklyPlanView].
 * 3. When: Always; tapping a workout day's header opens its detail view.
 */
@Composable
private fun DayPlanCard(
    day: DayPlan,
    onOpen: () -> Unit,
    onSwap: () -> Unit,
    onDelete: () -> Unit,
    onAdd: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceVariant)
            .border(1.dp, colors.outline, RoundedCornerShape(12.dp))
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
 * 1. What: The detail for one day's workout — read-only until "Begin workout", then editable
 *          sets/reps with checkboxes, ending in a completion summary.
 * 2. Who: Rendered by [LoadedContent] when a workout day is selected.
 * 3. When: After tapping a workout day; "Complete workout" and the back button both return to
 *    the weekly list.
 */
@Composable
private fun WorkoutDetailView(
    dayPlan: DayPlan,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    var phase by remember(dayPlan.day) { mutableStateOf(Phase.NotStarted) }
    var showAlterDialog by remember(dayPlan.day) { mutableStateOf(false) }
    val rows = remember(dayPlan.day) {
        dayPlan.exercises.map { ExerciseRowState(it.name, it.sets, it.reps) }
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
                }
            }
            items(rows) { row ->
                ExerciseRow(row = row, editable = phase == Phase.InProgress)
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (phase) {
                        Phase.NotStarted -> {
                            OutlinedButton(
                                onClick = { showAlterDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Alter daily workout") }
                            Button(
                                onClick = { phase = Phase.InProgress },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                            ) { Text("Begin workout", fontWeight = FontWeight.SemiBold) }
                        }
                        Phase.InProgress -> {
                            Button(
                                onClick = { phase = Phase.Completed },
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
                        Phase.Completed -> Unit
                    }
                }
            }
        }
    }

    if (showAlterDialog) {
        AlterWorkoutDialog(
            workoutTitle = dayPlan.title,
            onDismiss = { showAlterDialog = false },
            onConfirm = { showAlterDialog = false },
        )
    }
}

/**
 * 1. What: One exercise row — name and sets×reps; in progress it shows a checkbox and editable
 *          sets/reps fields next to the original goal.
 * 2. Who: Rendered for each exercise in [WorkoutDetailView].
 * 3. When: The checkbox/fields appear only after the user taps "Begin workout".
 */
@Composable
private fun ExerciseRow(row: ExerciseRowState, editable: Boolean) {
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
        if (editable) {
            Checkbox(checked = row.done, onCheckedChange = { row.done = it })
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
            if (editable) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NumberField(value = row.sets, onValueChange = { row.sets = it }, modifier = Modifier.width(64.dp))
                    Text("  ×  ", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                    NumberField(value = row.reps, onValueChange = { row.reps = it }, modifier = Modifier.width(64.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "(goal: ${row.goalSets} × ${row.goalReps})",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    text = "${row.goalSets} × ${row.goalReps}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * 1. What: Small numeric text field that accepts up to 3 digits.
 * 2. Who: Used for the sets and reps inputs in [ExerciseRow].
 * 3. When: Shown only while a workout is in progress.
 */
@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { new -> if (new.length <= 3 && new.all { it.isDigit() }) onValueChange(new) },
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
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
 * 1. What: Dialog offering ways to change today's workout (easier / harder / swap a day).
 * 2. Who: Shown by [WorkoutDetailView].
 * 3. When: When the user taps "Alter daily workout"; dismissed on confirm or cancel.
 */
@Composable
private fun AlterWorkoutDialog(
    workoutTitle: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val options = listOf("Make it easier", "Make it harder", "Swap a day")
    var selected by remember { mutableStateOf(options.last()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.onSurface,
        textContentColor = colors.onSurfaceVariant,
        title = { Text(workoutTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("How do you want to change this workout?", style = MaterialTheme.typography.bodyMedium)
                options.forEach { option ->
                    val isSelected = option == selected
                    Surface(
                        onClick = { selected = option },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) colors.surfaceContainerHighest else colors.surfaceVariant,
                        contentColor = if (isSelected) colors.onSurface else colors.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = option,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, shape = RoundedCornerShape(8.dp)) {
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
            onUpdateDay = { _, _ -> },
            onRegenerate = {},
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
            onBack = {},
        )
    }
}

/**
 * 1. What: Design-time preview of a single editable exercise row (in-progress state).
 * 2. Who: Called by Android Studio's Compose preview renderer.
 * 3. When: Rendered at design time in the IDE; never runs in the shipped app.
 */
@Preview(showBackground = true)
@Composable
private fun ExerciseRowPreview() {
    MyFitTheme {
        ExerciseRow(row = ExerciseRowState("Bench press", 3, 10), editable = true)
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