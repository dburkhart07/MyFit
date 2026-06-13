package com.example.myfit.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myfit.ui.components.BottomTab
import com.example.myfit.ui.common.DailyWorkout
import com.example.myfit.ui.components.MyFitBottomBar
import com.example.myfit.ui.components.MyFitTopBar
import com.example.myfit.ui.components.CompletionIcon
import com.example.myfit.ui.theme.Cyan500
import com.example.myfit.ui.theme.MyFitTheme
import com.example.myfit.ui.theme.Teal400

/** Dummy history data — replace with a real backend fetch later. */
private val DUMMY_WEEKS = listOf(
    WeekHistory(
        week = "Week of May 26",
        completed = 4,
        total = 5,
        dailyWorkouts = listOf(
            DailyWorkout("Mon", "Upper body", "5 / 5 exercises", completed = true),
            DailyWorkout("Wed", "Lower body", "6 / 6 exercises", completed = true),
            DailyWorkout("Thu", "Upper body", "5 / 5 exercises", completed = true),
            DailyWorkout("Sat", "Full body", "7 / 7 exercises", completed = true),
            DailyWorkout("Sun", "Cardio", "Missed", completed = false),
        ),
    ),
    WeekHistory(
        week = "Week of May 19",
        completed = 5,
        total = 5,
        dailyWorkouts = listOf(
            DailyWorkout("Mon", "Upper body", "5 / 5 exercises", completed = true),
            DailyWorkout("Tue", "Lower body", "6 / 6 exercises", completed = true),
            DailyWorkout("Wed", "Core", "4 / 4 exercises", completed = true),
            DailyWorkout("Fri", "Full body", "7 / 7 exercises", completed = true),
            DailyWorkout("Sat", "Cardio", "3 / 3 exercises", completed = true),
        ),
    ),
    WeekHistory(
        week = "Week of May 12",
        completed = 3,
        total = 4,
        dailyWorkouts = listOf(
            DailyWorkout("Mon", "Upper body", "5 / 5 exercises", completed = true),
            DailyWorkout("Wed", "Lower body", "6 / 6 exercises", completed = true),
            DailyWorkout("Fri", "Full body", "Missed", completed = false),
            DailyWorkout("Sat", "Core", "4 / 4 exercises", completed = true),
        ),
    ),
)

/**
 * 1. What: History tab — a list of training weeks that drills into each week's daily workouts.
 * 2. Who: Called by the app's NavHost (the History destination) once nav is wired.
 * 3. When: Shown when the user taps the History tab in the bottom bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onLogout: () -> Unit,
    onSelectTab: (BottomTab) -> Unit,
) {
    var selectedWeek by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = { MyFitTopBar(onLogout = onLogout) },
        bottomBar = { MyFitBottomBar(current = BottomTab.History, onSelect = onSelectTab) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        val selected = selectedWeek
        if (selected == null) {
            WeekListView(
                weeks = DUMMY_WEEKS,
                onWeekClick = { selectedWeek = it },
                modifier = Modifier.padding(padding),
            )
        } else {
            WeekDetailView(
                week = DUMMY_WEEKS[selected],
                onBack = { selectedWeek = null },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

/**
 * 1. What: The list of weeks, each a tappable card with a completion progress bar.
 * 2. Who: Rendered by [HistoryScreen] when no week is selected.
 * 3. When: The default state of the History tab.
 */
@Composable
private fun WeekListView(
    weeks: List<WeekHistory>,
    onWeekClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Click a week to view details →",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        itemsIndexed(weeks) { index, week ->
            WeekCard(week = week, onClick = { onWeekClick(index) })
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "💡 Tap any week above to see daily workout details",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * 1. What: A single week summary card — title, days completed, and a gradient progress bar.
 * 2. Who: Rendered for every entry in [WeekListView].
 * 3. When: Tapping it opens that week's detail view.
 */
@Composable
private fun WeekCard(week: WeekHistory, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = week.week,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "${week.completed} / ${week.total} days completed",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ProgressBar(fraction = if (week.total == 0) 0f else week.completed.toFloat() / week.total)
    }
}

/**
 * 1. What: A rounded track with a teal→cyan gradient fill showing week completion.
 * 2. Who: Used inside [WeekCard].
 * 3. When: Drawn for every week card; width tracks [fraction] (0f..1f).
 */
@Composable
private fun ProgressBar(fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(Teal400, Cyan500))),
        )
    }
}

/**
 * 1. What: A week's detail — a back button and each day's workout with a completion marker.
 * 2. Who: Rendered by [HistoryScreen] when a week is selected.
 * 3. When: After the user taps a week card; "Back to all weeks" returns to the list.
 */
@Composable
private fun WeekDetailView(
    week: WeekHistory,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TextButton(
                onClick = onBack,
                contentPadding = PaddingValues(0.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = "Back to all weeks",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = week.week,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${week.completed} of ${week.total} days completed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(week.dailyWorkouts) { workout ->
            DailyWorkoutCard(workout)
        }
    }
}

/**
 * 1. What: A single day's workout row — day · title, exercise count, and a completion icon.
 * 2. Who: Rendered for every entry in [WeekDetailView].
 * 3. When: Shown in a week's detail view; missed workouts show a gray X, completed a teal check.
 */
@Composable
fun DailyWorkoutCard(workout: DailyWorkout) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "${workout.day} · ${workout.title}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = workout.exercises,
                style = MaterialTheme.typography.bodySmall,
                color = if (workout.completed) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        }
        CompletionIcon(completed = workout.completed)
    }
}

/**
 * 1. What: Design-time preview of the History tab (week list).
 * 2. Who: Called by Android Studio's Compose preview renderer.
 * 3. When: Rendered at design time in the IDE; never runs in the shipped app.
 */
@Preview(showBackground = true)
@Composable
private fun HistoryScreenPreview() {
    MyFitTheme { HistoryScreen(onLogout = {}, onSelectTab = {}) }
}
