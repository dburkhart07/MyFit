package com.example.myfit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myfit.model.DayStatus
import com.example.myfit.ui.history.DailyWorkoutCard
import com.example.myfit.ui.theme.Cyan500
import com.example.myfit.ui.theme.MyFitTheme
import com.example.myfit.ui.theme.Teal400

/**
 * 1. What: The trailing status badge — a gradient check when fully done, an empty circle when
 *    only partially done, and a gray X when missed.
 * 2. Who: Used inside [DailyWorkoutCard] and the workout completion summary.
 * 3. When: Reflects a day's [DayStatus].
 */
@Composable
fun CompletionIcon(status: DayStatus) {
    when (status) {
        DayStatus.COMPLETED -> Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Teal400, Cyan500))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Completed",
                tint = MaterialTheme.colorScheme.onTertiary,
                modifier = Modifier.size(20.dp),
            )
        }

        DayStatus.MISSED -> Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Missed",
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(16.dp),
            )
        }

        // Partial (or not-yet-due) — an empty outlined circle, no inner icon.
        DayStatus.PARTIAL, DayStatus.PENDING -> Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
        )
    }
}

/** Convenience overload — a simple done/not-done badge (true → completed, false → missed). */
@Composable
fun CompletionIcon(completed: Boolean) {
    CompletionIcon(status = if (completed) DayStatus.COMPLETED else DayStatus.MISSED)
}

/**
 * 1. What: Design-time preview of the completed (gradient check) state.
 * 2. Who: Called by Android Studio's Compose preview renderer.
 * 3. When: Rendered at design time in the IDE; never runs in the shipped app.
 */
@Preview(showBackground = true)
@Composable
private fun CompletionIconCompletedPreview() {
    MyFitTheme {
        CompletionIcon(completed = true)
    }
}

/**
 * 1. What: Design-time preview of the missed (gray X) state.
 * 2. Who: Called by Android Studio's Compose preview renderer.
 * 3. When: Rendered at design time in the IDE; never runs in the shipped app.
 */
@Preview(showBackground = true)
@Composable
private fun CompletionIconMissedPreview() {
    MyFitTheme {
        CompletionIcon(completed = false)
    }
}

/**
 * 1. What: Design-time preview of the partial (empty circle) state.
 * 2. Who: Called by Android Studio's Compose preview renderer.
 * 3. When: Rendered at design time in the IDE; never runs in the shipped app.
 */
@Preview(showBackground = true)
@Composable
private fun CompletionIconPartialPreview() {
    MyFitTheme {
        CompletionIcon(status = DayStatus.PARTIAL)
    }
}