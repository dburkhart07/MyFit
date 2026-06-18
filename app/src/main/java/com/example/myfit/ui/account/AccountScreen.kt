package com.example.myfit.ui.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myfit.auth.AuthViewModel
import com.example.myfit.model.Equipment
import com.example.myfit.model.ExperienceLevel
import com.example.myfit.model.Goal
import com.example.myfit.model.OnboardingPreferences
import com.example.myfit.model.ProfileStats
import com.example.myfit.ui.components.BottomTab
import com.example.myfit.ui.components.MyFitBottomBar
import com.example.myfit.ui.components.MyFitTopBar
import com.example.myfit.ui.onboarding.OnboardingUiState
import com.example.myfit.ui.onboarding.OnboardingViewModel
import com.example.myfit.ui.onboarding.PreferencesUiState
import com.example.myfit.ui.theme.MyFitTheme

private const val NOT_SET = "Not set"
private val GOAL_OPTIONS = Goal.entries.map { it.label }
private val DAYS_OPTIONS = OnboardingPreferences.DAYS_RANGE.map { it.toString() }
private val EQUIPMENT_OPTIONS = Equipment.entries.map { it.label }
private val EXPERIENCE_OPTIONS = ExperienceLevel.entries.map { it.label }

/**
 * 1. What: Account tab — profile card plus an editable "Workout info" section.
 * 2. Who: Called by the app's NavHost (the Account destination).
 * 3. When: Shown when the user taps the Account tab in the bottom bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onLogout: () -> Unit,
    onSelectTab: (BottomTab) -> Unit,
    viewModel: AuthViewModel = viewModel(),
    preferencesViewModel: OnboardingViewModel = viewModel(),
    statsViewModel: AccountStatsViewModel = viewModel(),
) {
    val prefsState by preferencesViewModel.preferences.collectAsStateWithLifecycle()
    val saveState by preferencesViewModel.uiState.collectAsStateWithLifecycle()
    val stats by statsViewModel.stats.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        preferencesViewModel.loadPreferences()
        statsViewModel.load()
    }

    Scaffold(
        topBar = { MyFitTopBar(onLogout = onLogout) },
        bottomBar = { MyFitBottomBar(current = BottomTab.Account, onSelect = onSelectTab) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            UserInfoCard(name = viewModel.displayName, stats = stats)
            WorkoutInfoSection(
                state = prefsState,
                saveState = saveState,
                onRetry = { preferencesViewModel.loadPreferences() },
                onSave = { prefs -> preferencesViewModel.savePreferences(prefs) },
                onSaveHandled = { preferencesViewModel.resetState() },
            )
        }
    }
}

/**
 * 1. What: Gradient header card with the user's avatar, name, and real workout stats (completed
 *    workouts + week streak); shows a placeholder line until [stats] loads.
 * 2. Who: Rendered at the top of [AccountScreen].
 * 3. When: Always; [name] is the signed-in user's display name, [stats] come from the repository.
 */
@Composable
private fun UserInfoCard(name: String, stats: ProfileStats?) {
    val colors = MaterialTheme.colorScheme
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val statsText = if (stats == null) {
        "Loading stats…"
    } else {
        "${stats.completedWorkouts} completed workouts · ${stats.weekStreak} wk streak"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(listOf(colors.surfaceVariant, colors.surfaceContainerHighest))
            )
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(12.dp))
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(colors.secondary, colors.tertiary))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initial,
                color = colors.onTertiary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
            )
            Text(
                text = statsText,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

/**
 * 1. What: The "Workout info" section — shows the user's stored preferences pulled from
 *    Firestore, with loading/error states and an Edit toggle into an edit form.
 * 2. Who: Rendered below the profile card in [AccountScreen]; driven by [state].
 * 3. When: Once loaded, displays the saved goal/days/equipment/experience. Edit reveals
 *    dropdowns/chips (mirroring onboarding); Cancel and Update both return to the
 *    read-only view (persisting the edit is not wired up yet).
 */
@Composable
private fun WorkoutInfoSection(
    state: PreferencesUiState,
    saveState: OnboardingUiState,
    onRetry: () -> Unit,
    onSave: (OnboardingPreferences) -> Unit,
    onSaveHandled: () -> Unit,
) {
    val prefs = (state as? PreferencesUiState.Loaded)?.preferences

    var isEditing by remember { mutableStateOf(false) }
    var editGoal by remember { mutableStateOf(GOAL_OPTIONS.first()) }
    var editDays by remember { mutableIntStateOf(DAYS_OPTIONS.first().toInt()) }
    var editExperience by remember { mutableStateOf(EXPERIENCE_OPTIONS.first()) }
    val editEquipment = remember { mutableStateListOf<String>() }

    val isSaving = saveState is OnboardingUiState.Saving

    // Once the save succeeds, leave edit mode and clear the transient save state.
    LaunchedEffect(saveState) {
        if (saveState is OnboardingUiState.Saved) {
            isEditing = false
            onSaveHandled()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Workout info",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            // Editing only makes sense once the data has loaded.
            if (prefs != null && !isEditing) {
                TextButton(
                    onClick = {
                        editGoal = prefs.goal.label
                        editDays = prefs.daysPerWeek
                        editExperience = prefs.experience.label
                        editEquipment.clear()
                        editEquipment.addAll(prefs.equipment.map { it.label })
                        isEditing = true
                    },
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(
                        text = "Edit",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }

        when (state) {
            is PreferencesUiState.Loading -> LoadingBox()

            is PreferencesUiState.Error -> ErrorBox(message = state.message, onRetry = onRetry)

            is PreferencesUiState.Loaded -> {
                FieldLabel("Goal")
                if (isEditing) {
                    DropdownField(value = editGoal, options = GOAL_OPTIONS, onSelect = { editGoal = it })
                } else {
                    ValueBox(prefs?.goal?.label ?: NOT_SET)
                }

                FieldLabel("Days / week")
                if (isEditing) {
                    DropdownField(
                        value = editDays.toString(),
                        options = DAYS_OPTIONS,
                        onSelect = { editDays = it.toInt() },
                    )
                } else {
                    ValueBox(prefs?.daysPerWeek?.toString() ?: NOT_SET)
                }

                FieldLabel("Equipment")
                if (isEditing) {
                    EquipmentBox(
                        selected = editEquipment,
                        onToggle = { item ->
                            if (editEquipment.contains(item)) editEquipment.remove(item)
                            else editEquipment.add(item)
                        },
                    )
                } else {
                    ValueBox(prefs?.equipment?.joinToString(", ") { it.label } ?: NOT_SET)
                }

                FieldLabel("Experience")
                if (isEditing) {
                    DropdownField(
                        value = editExperience,
                        options = EXPERIENCE_OPTIONS,
                        onSelect = { editExperience = it },
                    )
                } else {
                    ValueBox(prefs?.experience?.label ?: NOT_SET)
                }

                if (isEditing) {
                    if (saveState is OnboardingUiState.Error) {
                        Text(
                            text = saveState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = {
                                isEditing = false
                                onSaveHandled()
                            },
                            enabled = !isSaving,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        ) {
                            Text("Cancel", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                onSave(
                                    OnboardingPreferences(
                                        goal = Goal.fromLabel(editGoal) ?: Goal.entries.first(),
                                        daysPerWeek = editDays,
                                        equipment = editEquipment.mapNotNull { Equipment.fromLabel(it) }
                                            .ifEmpty { listOf(Equipment.NONE) },
                                        experience = ExperienceLevel.fromLabel(editExperience)
                                            ?: ExperienceLevel.entries.first(),
                                    )
                                )
                            },
                            enabled = !isSaving,
                            modifier = Modifier.weight(1f),
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text("Update info", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 1. What: Centered spinner shown while the stored preferences are being fetched.
 * 2. Who: Rendered by [WorkoutInfoSection] in its Loading state.
 * 3. When: Between the screen appearing and Firestore returning the user's document.
 */
@Composable
private fun LoadingBox() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp))
    }
}

/**
 * 1. What: Error message plus a Retry action shown when the preferences fail to load.
 * 2. Who: Rendered by [WorkoutInfoSection] in its Error state.
 * 3. When: When the Firestore read fails (e.g. no signed-in user or network error).
 */
@Composable
private fun ErrorBox(message: String, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onRetry, contentPadding = PaddingValues(0.dp)) {
            Text(
                text = "Retry",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

/**
 * 1. What: Small muted label rendered above each workout-info field.
 * 2. Who: Used by [WorkoutInfoSection] for the Goal, Days/week, and Equipment fields.
 * 3. When: Always shown, in both the read-only and edit states.
 */
@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * 1. What: Read-only field value rendered as a bordered gray box.
 * 2. Who: Used by [WorkoutInfoSection] to display a field's current value.
 * 3. When: Shown for each field while not editing; the edit state swaps in inputs instead.
 */
@Composable
private fun ValueBox(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * 1. What: A read-only "select"-style dropdown, mirroring the OnboardingScreen's DropdownField.
 * 2. Who: Used for Goal and Days/week in [WorkoutInfoSection]'s edit mode.
 * 3. When: Only shown while editing; picking an option calls [onSelect].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * 1. What: Wrap-flowing equipment toggle chips, mirroring the OnboardingScreen's chip set.
 * 2. Who: Used in [WorkoutInfoSection]'s edit mode.
 * 3. When: Tapping a box calls [onToggle] for that item; [selected] drives the filled state.
 */
@Composable
private fun EquipmentBox(
    selected: List<String>,
    onToggle: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EQUIPMENT_OPTIONS.forEach { item ->
            val isSelected = item in selected
            FilterChip(
                selected = isSelected,
                onClick = { onToggle(item) },
                label = { Text(item) },
                shape = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = colors.surfaceVariant,
                    labelColor = colors.onSurfaceVariant,
                    selectedContainerColor = colors.primary,
                    selectedLabelColor = colors.onPrimary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = colors.outline,
                    selectedBorderColor = colors.primary,
                ),
            )
        }
    }
}

/**
 * 1. What: Design-time preview of the Account tab.
 * 2. Who: Called by Android Studio's Compose preview renderer.
 * 3. When: Rendered at design time in the IDE; never runs in the shipped app.
 */
@Preview(showBackground = true)
@Composable
private fun AccountScreenPreview() {
    MyFitTheme { AccountScreen(onLogout = {}, onSelectTab = {}) }
}

/**
 * 1. What: Design-time preview of the profile header card with a dummy name.
 * 2. Who: Called by Android Studio's Compose preview renderer.
 * 3. When: Rendered at design time in the IDE; never runs in the shipped app.
 */
@Preview(showBackground = true)
@Composable
private fun UserInfoCardPreview() {
    MyFitTheme {
        UserInfoCard(name = "Dalton Burkhart", stats = ProfileStats(completedWorkouts = 12, weekStreak = 3))
    }
}

/**
 * 1. What: Design-time preview of the editable "Workout info" section (read-only state).
 * 2. Who: Called by Android Studio's Compose preview renderer.
 * 3. When: Rendered at design time in the IDE; never runs in the shipped app.
 */
@Preview(showBackground = true)
@Composable
private fun WorkoutInfoSectionPreview() {
    MyFitTheme {
        WorkoutInfoSection(
            state = PreferencesUiState.Loaded(
                OnboardingPreferences(
                    goal = Goal.BUILD_MUSCLE,
                    daysPerWeek = 4,
                    equipment = listOf(Equipment.DUMBBELLS, Equipment.BANDS),
                    experience = ExperienceLevel.INTERMEDIATE,
                )
            ),
            saveState = OnboardingUiState.Idle,
            onRetry = {},
            onSave = {},
            onSaveHandled = {},
        )
    }
}

/**
 * 1. What: Design-time preview of a single dropdown field with dummy goal options.
 * 2. Who: Called by Android Studio's Compose preview renderer.
 * 3. When: Rendered at design time in the IDE; never runs in the shipped app.
 */
@Preview(showBackground = true)
@Composable
private fun DropdownFieldPreview() {
    MyFitTheme {
        DropdownField(value = GOAL_OPTIONS.first(), options = GOAL_OPTIONS, onSelect = {})
    }
}

/**
 * 1. What: Design-time preview of the equipment toggle chips with dummy selections.
 * 2. Who: Called by Android Studio's Compose preview renderer.
 * 3. When: Rendered at design time in the IDE; never runs in the shipped app.
 */
@Preview(showBackground = true)
@Composable
private fun EquipmentChipsPreview() {
    MyFitTheme {
        EquipmentBox(selected = listOf("Dumbbells", "Bands"), onToggle = {})
    }
}

/**
 * 1. What: Design-time preview of a read-only value box with dummy text.
 * 2. Who: Called by Android Studio's Compose preview renderer.
 * 3. When: Rendered at design time in the IDE; never runs in the shipped app.
 */
@Preview(showBackground = true)
@Composable
private fun ValueBoxPreview() {
    MyFitTheme {
        ValueBox(Goal.BUILD_MUSCLE.label)
    }
}
