package com.example.myfit.ui.history

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myfit.model.DayStatus
import com.example.myfit.ui.common.DailyWorkout
import com.example.myfit.ui.common.BottomTab
import com.example.myfit.ui.common.CompletionIcon
import com.example.myfit.ui.common.MyFitBottomBar
import com.example.myfit.ui.common.MyFitTopBar
import com.example.myfit.ui.theme.Cyan500
import com.example.myfit.ui.theme.MyFitTheme
import com.example.myfit.ui.theme.Teal400

/**
 * 1. What: History tab — a list of finished training weeks (most recent first) that drills into
 *    each week's daily workouts. Backed by real archived data from [HistoryViewModel].
 * 2. Who: Called by the app's NavHost (the History destination).
 * 3. When: Shown when the user taps the History tab in the bottom bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onLogout: () -> Unit,
    onSelectTab: (BottomTab) -> Unit,
    viewModel: HistoryViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedWeek by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = { MyFitTopBar(onLogout = onLogout) },
        bottomBar = { MyFitBottomBar(current = BottomTab.History, onSelect = onSelectTab) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (val state = uiState) {
            is HistoryUiState.Loading -> StatusBox(message = "Loading your history…", modifier = modifier)

            is HistoryUiState.Error ->
                ErrorBox(message = state.message, onRetry = { viewModel.load() }, modifier = modifier)

            is HistoryUiState.Loaded -> {
                val weeks = state.weeks
                val selected = selectedWeek
                when {
                    weeks.isEmpty() -> EmptyHistory(modifier = modifier)
                    selected == null || selected !in weeks.indices ->
                        WeekListView(
                            weeks = weeks,
                            onWeekClick = { selectedWeek = it },
                            modifier = modifier,
                        )
                    else ->
                        WeekDetailView(
                            week = weeks[selected],
                            onBack = { selectedWeek = null },
                            modifier = modifier,
                        )
                }
            }
        }
    }
}

/**
 * 1. What: The list of weeks, each a tappable card with a completion progress bar.
 * 2. Who: Rendered by [HistoryScreen] when weeks exist and none is selected.
 * 3. When: The default state of the History tab once data has loaded.
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
 * 3. When: Shown in a week's detail view; missed workouts show a gray X, partial an empty circle,
 *    and fully-completed a teal check.
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
                text = workout.label,
                style = MaterialTheme.typography.bodySmall,
                color = if (workout.status == DayStatus.MISSED) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        CompletionIcon(status = workout.status)
    }
}

/**
 * 1. What: Centered status box with a spinner and a message (the Loading state).
 * 2. Who: Rendered by [HistoryScreen] while history is being read from Firestore.
 * 3. When: On first load.
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
 * 1. What: Friendly empty state shown until the user finishes their first week.
 * 2. Who: Rendered by [HistoryScreen] when there are no archived weeks.
 * 3. When: New accounts, or before any week has elapsed.
 */
@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "No completed weeks yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Finish a week of workouts and it'll show up here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * 1. What: Error message plus a Retry action.
 * 2. Who: Rendered by [HistoryScreen] in the Error state.
 * 3. When: When loading history fails.
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

/* ----------------------------- Previews ----------------------------- */

private val SAMPLE_WEEKS = listOf(
    WeekHistory(
        week = "Week of Jun 9",
        completed = 3,
        total = 5,
        dailyWorkouts = listOf(
            DailyWorkout("Mon", "Upper body", completed = 5, total = 5),
            DailyWorkout("Tue", "Lower body", completed = 3, total = 6),
            DailyWorkout("Thu", "Upper body", completed = 0, total = 5),
            DailyWorkout("Sat", "Full body", completed = 7, total = 7),
            DailyWorkout("Sun", "Core", completed = 4, total = 4),
        ),
    ),
    WeekHistory(
        week = "Week of Jun 2",
        completed = 5,
        total = 5,
        dailyWorkouts = listOf(
            DailyWorkout("Mon", "Upper body", completed = 5, total = 5),
            DailyWorkout("Wed", "Lower body", completed = 6, total = 6),
        ),
    ),
)

@Preview(showBackground = true)
@Composable
private fun WeekListViewPreview() {
    MyFitTheme { WeekListView(weeks = SAMPLE_WEEKS, onWeekClick = {}) }
}

@Preview(showBackground = true)
@Composable
private fun WeekCardPreview() {
    MyFitTheme { WeekCard(week = SAMPLE_WEEKS[0], onClick = {}) }
}

@Preview(showBackground = true)
@Composable
private fun WeekDetailViewPreview() {
    MyFitTheme { WeekDetailView(week = SAMPLE_WEEKS[0], onBack = {}) }
}

@Preview(showBackground = true)
@Composable
private fun DailyWorkoutCardPreview() {
    MyFitTheme {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
            DailyWorkoutCard(DailyWorkout("Mon", "Upper body", completed = 5, total = 5))
            DailyWorkoutCard(DailyWorkout("Tue", "Lower body", completed = 3, total = 6))
            DailyWorkoutCard(DailyWorkout("Thu", "Cardio", completed = 0, total = 5))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyHistoryPreview() {
    MyFitTheme { EmptyHistory() }
}

@Preview(showBackground = true)
@Composable
private fun ProgressBarPreview() {
    MyFitTheme { ProgressBar(fraction = 0.6f) }
}

@Preview(showBackground = true)
@Composable
private fun StatusBoxPreview() {
    MyFitTheme { StatusBox(message = "Loading your history…") }
}

@Preview(showBackground = true)
@Composable
private fun ErrorBoxPreview() {
    MyFitTheme { ErrorBox(message = "Could not load your history", onRetry = {}) }
}
