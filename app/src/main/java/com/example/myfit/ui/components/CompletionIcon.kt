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
import androidx.compose.ui.unit.dp
import com.example.myfit.ui.history.DailyWorkoutCard
import com.example.myfit.ui.theme.Cyan500
import com.example.myfit.ui.theme.Teal400

/**
 * 1. What: The trailing status badge — a gradient check when done, a gray X when missed.
 * 2. Who: Used inside [DailyWorkoutCard].
 * 3. When: Reflects each workout's [completed] flag.
 */
@Composable
fun CompletionIcon(completed: Boolean) {
    if (completed) {
        Box(
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
    } else {
        Box(
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
    }
}