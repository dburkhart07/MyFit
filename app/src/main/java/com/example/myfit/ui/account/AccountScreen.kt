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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myfit.auth.AuthViewModel
import com.example.myfit.ui.components.BottomTab
import com.example.myfit.ui.components.MyFitBottomBar
import com.example.myfit.ui.components.MyFitTopBar
import com.example.myfit.ui.theme.MyFitTheme

private const val PROFILE_GOAL = "Build muscle"
private const val PROFILE_DAYS_PER_WEEK = 4
private val PROFILE_EQUIPMENT = listOf("Dumbbells", "Bands")
private const val PROFILE_STATS = "12 workouts · 3 wk streak"
private val GOAL_OPTIONS = listOf("Build muscle", "Lose weight", "General fitness", "Improve endurance")
private val DAYS_OPTIONS = (1..7).map { it.toString() }
private val EQUIPMENT_OPTIONS = listOf("Dumbbells", "Bodyweight", "Bands", "Barbell", "Kettlebell", "None")

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
) {
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
            UserInfoCard(name = viewModel.displayName)
            WorkoutInfoSection()
        }
    }
}

/**
 * 1. What: Gradient header card with the user's avatar, name, and workout stats.
 * 2. Who: Rendered at the top of [AccountScreen].
 * 3. When: Always; [name] is the signed-in user's display name, stats are dummy data.
 */
@Composable
private fun UserInfoCard(name: String) {
    val colors = MaterialTheme.colorScheme
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
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
                text = PROFILE_STATS,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

/**
 * 1. What: The "Workout info" section — read-only fields with an Edit toggle into an edit form.
 * 2. Who: Rendered below the profile card in [AccountScreen].
 * 3. When: Edit reveals dropdowns/chips (mirroring onboarding); Cancel and Update both discard
 *    changes and return to the read-only view.
 */
@Composable
private fun WorkoutInfoSection() {
    var isEditing by remember { mutableStateOf(false) }
    var editGoal by remember { mutableStateOf(PROFILE_GOAL) }
    var editDays by remember { mutableIntStateOf(PROFILE_DAYS_PER_WEEK) }
    val editEquipment = remember { mutableStateListOf<String>().apply { addAll(PROFILE_EQUIPMENT) } }

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
            if (!isEditing) {
                TextButton(
                    onClick = {
                        editGoal = PROFILE_GOAL
                        editDays = PROFILE_DAYS_PER_WEEK
                        editEquipment.clear()
                        editEquipment.addAll(PROFILE_EQUIPMENT)
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

        FieldLabel("Goal")
        if (isEditing) {
            DropdownField(value = editGoal, options = GOAL_OPTIONS, onSelect = { editGoal = it })
        } else {
            ValueBox(PROFILE_GOAL)
        }

        FieldLabel("Days / week")
        if (isEditing) {
            DropdownField(
                value = editDays.toString(),
                options = DAYS_OPTIONS,
                onSelect = { editDays = it.toInt() },
            )
        } else {
            ValueBox(PROFILE_DAYS_PER_WEEK.toString())
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
            ValueBox(PROFILE_EQUIPMENT.joinToString(", "))
        }

        if (isEditing) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { isEditing = false },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
                // TODO: Update logic on submission
                Button(
                    onClick = { isEditing = false },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Update info", fontWeight = FontWeight.SemiBold)
                }
            }
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
        UserInfoCard(name = "Dalton Burkhart")
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
        WorkoutInfoSection()
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
        EquipmentBox(selected = PROFILE_EQUIPMENT, onToggle = {})
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
        ValueBox(PROFILE_GOAL)
    }
}
