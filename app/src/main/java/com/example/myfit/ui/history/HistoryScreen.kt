package com.example.myfit.ui.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.myfit.ui.common.BottomTab
import com.example.myfit.ui.common.MyFitBottomBar
import com.example.myfit.ui.theme.MyFitTheme

/**
 * 1. What: History tab — placeholder content with the shared top bar and bottom bar.
 * 2. Who: Called by the app's NavHost (the History destination) once nav is wired.
 * 3. When: Shown when the user taps the History tab in the bottom bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onSelectTab: (BottomTab) -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("History", fontWeight = FontWeight.Bold) }) },
        bottomBar = { MyFitBottomBar(current = BottomTab.History, onSelect = onSelectTab) },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text("Hello World — History", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

/**
 * 1. What: Design-time preview of the History tab.
 * 2. Who: Called by Android Studio's Compose preview renderer.
 * 3. When: Rendered at design time in the IDE; never runs in the shipped app.
 */
@Preview(showBackground = true)
@Composable
private fun HistoryScreenPreview() {
    MyFitTheme { HistoryScreen(onSelectTab = {}) }
}