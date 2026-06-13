package com.example.myfit.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.example.myfit.ui.theme.MyFitTheme

/**
 * The three destinations the bottom bar switches between. Swap the icons for
 * FitnessCenter / History if you add the material-icons-extended dependency.
 */
enum class BottomTab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Filled.Home),
    History("History", Icons.Filled.AccessTime),
    Account("Account", Icons.Filled.Person),
}

/**
 * 1. What: Reusable Material 3 bottom navigation bar showing the three app tabs.
 * 2. Who: Called by every main screen's Scaffold (WeeklyHomeScreen, HistoryScreen, AccountScreen).
 * 3. When: Rendered as the bottom bar of those screens; onSelect fires when a tab is tapped.
 */
@Composable
fun MyFitBottomBar(
    current: BottomTab,
    onSelect: (BottomTab) -> Unit,
) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        NavigationBar {
            BottomTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = tab == current,
                    onClick = { onSelect(tab) },
                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                    label = { Text(tab.label) },
                )
            }
        }
    }
}

/**
 * 1. What: Design-time preview of the bottom bar with the Home tab selected.
 * 2. Who: Called by Android Studio's Compose preview renderer.
 * 3. When: Rendered at design time in the IDE; never runs in the shipped app.
 */
@Preview(showBackground = true)
@Composable
private fun MyFitBottomBarPreview() {
    MyFitTheme {
        MyFitBottomBar(current = BottomTab.Home, onSelect = {})
    }
}