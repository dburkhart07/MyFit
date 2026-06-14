package com.example.myfit.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myfit.ui.theme.MyFitTheme

private val GOAL_OPTIONS = listOf("Build muscle", "Lose weight", "General fitness", "Improve endurance")
private val DAY_OPTIONS = (1..7).toList()
private val EQUIPMENT_OPTIONS = listOf("Dumbbells", "Bodyweight", "Bands", "Barbell", "Kettlebell", "None")
private val EXPERIENCE_OPTIONS = listOf("Beginner", "Intermediate", "Advanced", "Expert")

/**
 * 1. What: Onboarding screen — collects goal, days/week, equipment, and experience over a gradient
 *          backdrop, then a confirmation dialog before the first plan is generated.
 * 2. Who: Called by the app's NavHost (the Onboarding destination).
 * 3. When: Shown once right after sign-up; tapping through the confirmation dialog invokes
 *    [onSubmit], which routes to the Workouts tab. Selections are local dummy state for now.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onSubmit: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    var goal by rememberSaveable { mutableStateOf(GOAL_OPTIONS.first()) }
    var daysPerWeek by rememberSaveable { mutableStateOf(4) }
    val equipment = remember { mutableStateListOf("Dumbbells") }
    var experience by rememberSaveable { mutableStateOf(EXPERIENCE_OPTIONS.first()) }
    var showConfirm by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(colors.background, colors.surface, colors.background)
                )
            ),
        containerColor = Color.Transparent,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(modifier = Modifier.height(8.dp))

            Text(
                text = "Set up your workouts",
                color = colors.onBackground,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "We'll create a personalized plan just for you",
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )

            Text(
                text = "Goal",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            DropdownField(
                value = goal,
                options = GOAL_OPTIONS,
                onSelect = { goal = it },
            )

            Text(
                text = "Days per week",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            DropdownField(
                value = daysPerWeek.toString(),
                options = DAY_OPTIONS.map { it.toString() },
                onSelect = { daysPerWeek = it.toInt() },
            )

            Text(
                text = "Equipment",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EQUIPMENT_OPTIONS.forEach { item ->
                    val selected = item in equipment
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (selected) equipment.remove(item) else equipment.add(item)
                        },
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
                            selected = selected,
                            borderColor = colors.outline,
                            selectedBorderColor = colors.primary,
                        ),
                    )
                }
            }

            Text(
                text = "Experience Level",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EXPERIENCE_OPTIONS.forEach { level ->
                    val selected = experience == level
                    Surface(
                        onClick = { experience = level },
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) colors.surfaceContainerHighest else colors.surfaceVariant,
                        contentColor = if (selected) colors.onSurface else colors.onSurfaceVariant,
                        border = BorderStroke(
                            1.dp,
                            if (selected) colors.secondary else colors.outline,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = level,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                        )
                    }
                }
            }

            Box(modifier = Modifier.height(4.dp))

            Button(
                onClick = { showConfirm = true },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text("Submit", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showConfirm) {
        val checkColor = colors.onTertiary
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            containerColor = colors.surface,
            titleContentColor = colors.onSurface,
            textContentColor = colors.onSurfaceVariant,
            icon = {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(colors = listOf(colors.secondary, colors.tertiary))
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.size(32.dp)) {
                        val s = size.minDimension / 24f
                        val checkPath = Path().apply {
                            moveTo(5f * s, 13f * s)
                            lineTo(9f * s, 17f * s)
                            lineTo(19f * s, 7f * s)
                        }
                        drawPath(
                            path = checkPath,
                            color = checkColor,
                            style = Stroke(
                                width = 2.5f * s,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "You're all set!",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = "Your first week is ready.",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            },
            confirmButton = {
                Button(
                    // TODO: persist the onboarding selections to the backend here before navigating.
                    onClick = onSubmit,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text("Go to home page", fontWeight = FontWeight.SemiBold)
                }
            },
        )
    }
}

/**
 * 1. What: A read-only "select"-style dropdown backed by an [ExposedDropdownMenuBox].
 * 2. Who: Used for the Goal and Days-per-week fields in [OnboardingScreen].
 * 3. When: Tapping it expands the option list; picking an option calls [onSelect] and collapses.
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
 * 1. What: Design-time preview of the Onboarding screen with an empty submit callback.
 * 2. Who: Called by Android Studio's Compose preview renderer.
 * 3. When: Rendered at design time in the IDE; never runs in the shipped app.
 */
@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    MyFitTheme {
        OnboardingScreen(onSubmit = {})
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
