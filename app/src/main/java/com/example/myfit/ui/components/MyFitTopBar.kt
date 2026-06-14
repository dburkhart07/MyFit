package com.example.myfit.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myfit.auth.AuthViewModel
import com.example.myfit.ui.theme.MyFitTheme

/**
 * 1. What: Shared app top bar — "MyFit" on the left, a logout button on the right.
 * 2. Who: Called by every main screen's Scaffold (WorkoutsScreen, HistoryScreen, AccountScreen).
 * 3. When: Rendered as the top bar of those screens; the logout button signs the user out
 *    (Firebase sign-out via [AuthViewModel]) and then invokes [onLogout] to navigate away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFitTopBar(
    onLogout: () -> Unit,
    viewModel: AuthViewModel = viewModel(),
) {
    Column {
        TopAppBar(
            title = { Text("MyFit", fontWeight = FontWeight.Bold) },
            actions = {
                IconButton(
                    onClick = {
                        viewModel.logout()
                        onLogout()
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Log out",
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
    }
}

/**
 * 1. What: Design-time preview of the shared top bar.
 * 2. Who: Called by Android Studio's Compose preview renderer.
 * 3. When: Rendered at design time in the IDE; never runs in the shipped app.
 */
@Preview(showBackground = true)
@Composable
private fun MyFitTopBarPreview() {
    MyFitTheme {
        MyFitTopBar(onLogout = {})
    }
}
