package com.example.myfit.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myfit.auth.AuthViewModel
import com.example.myfit.ui.components.BottomTab
import com.example.myfit.ui.components.MyFitBottomBar
import com.example.myfit.ui.components.MyFitTopBar

/**
 * 1. What: Home tab — greets the signed-in user with the shared top bar and bottom bar.
 * 2. Who: Called by the app's NavHost (the Home destination).
 * 3. When: Shown after login/onboarding and when the Home tab is selected.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onSelectTab: (BottomTab) -> Unit,
    viewModel: AuthViewModel = viewModel(),
) {
    Scaffold(
        topBar = { MyFitTopBar(onLogout = onLogout) },
        bottomBar = { MyFitBottomBar(current = BottomTab.Workouts, onSelect = onSelectTab) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Hello, ${viewModel.displayName}",
                style = MaterialTheme.typography.headlineLarge,
            )
        }
    }
}
