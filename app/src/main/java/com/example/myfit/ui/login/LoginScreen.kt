package com.example.myfit.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myfit.auth.AuthUiState
import com.example.myfit.auth.AuthViewModel
import com.example.myfit.ui.components.Logo
import com.example.myfit.ui.theme.MyFitTheme

/**
 * 1. What: Login screen — email/password fields with inline validation, a gradient backdrop,
 *          and a link over to sign up. A failed attempt surfaces an alert dialog.
 * 2. Who: Called by the app's NavHost (the Login destination); the start destination when no
 *    Firebase session exists.
 * 3. When: Shown on launch for signed-out users; on success [onLoginSuccess] navigates to the
 *    Workouts tab, [onNavigateToSignup] opens the Signup screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToSignup: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel(),
) {
    val colors = MaterialTheme.colorScheme
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showAlertDialog by rememberSaveable { mutableStateOf(false) }
    val isLoading = uiState is AuthUiState.Loading

    val isFormValid = AuthViewModel.isValidEmail(email) && password.isNotBlank()
    val emailError = email.isNotBlank() && !AuthViewModel.isValidEmail(email)

    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.Success -> onLoginSuccess()
            is AuthUiState.Error -> showAlertDialog = true
            else -> Unit
        }
    }

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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Logo()

            Text(
                text = "Log in",
                color = colors.onBackground,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            Box(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                placeholder = { Text("your.email@example.com") },
                singleLine = true,
                isError = emailError,
                supportingText = if (emailError) {
                    { Text("Email must be valid format") }
                } else {
                    null
                },
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = { viewModel.login(email, password) },
                enabled = !isLoading && isFormValid,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = colors.onPrimary,
                    )
                } else {
                    Text("Log in", fontWeight = FontWeight.SemiBold)
                }
            }

            TextButton(
                onClick = onNavigateToSignup,
                enabled = !isLoading,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.secondary),
            ) {
                Text("Don't have an account? Sign up")
            }
        }
    }

    if (showAlertDialog) {
        AlertDialog(
            onDismissRequest = {
                showAlertDialog = false
                viewModel.resetState()
            },
            containerColor = colors.surface,
            titleContentColor = colors.onSurface,
            title = {
                Text(
                    text = "Login failed",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            },
            confirmButton = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextButton(
                        onClick = {
                            showAlertDialog = false
                            viewModel.resetState()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.secondary),
                    ) {
                        Text("OK")
                    }
                }
            },
        )
    }
}

/**
 * 1. What: Design-time preview of the Login screen with empty navigation callbacks.
 * 2. Who: Called by Android Studio's Compose preview renderer.
 * 3. When: Rendered at design time in the IDE; never runs in the shipped app.
 */
@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    MyFitTheme {
        LoginScreen(onNavigateToSignup = {}, onLoginSuccess = {})
    }
}
