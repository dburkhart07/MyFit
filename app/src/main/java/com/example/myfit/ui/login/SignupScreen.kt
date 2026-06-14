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
import androidx.compose.ui.text.input.KeyboardCapitalization
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
 * 1. What: Sign-up screen — first/last name, email, and password + confirm fields with inline
 *          validation over a gradient backdrop, plus a link back to login. A failed attempt
 *          surfaces an alert dialog.
 * 2. Who: Called by the app's NavHost (the Signup destination).
 * 3. When: Reached from the Login screen; on success [onSignupSuccess] routes to onboarding,
 *    [onNavigateToLogin] returns to the Login screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    onNavigateToLogin: () -> Unit,
    onSignupSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel(),
) {
    val colors = MaterialTheme.colorScheme
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showAlertDialog by rememberSaveable { mutableStateOf(false) }
    val isLoading = uiState is AuthUiState.Loading

    val passwordsMatch = confirmPassword.isNotBlank() && password == confirmPassword
    val isFormValid = firstName.isNotBlank() &&
        lastName.isNotBlank() &&
        AuthViewModel.isValidEmail(email) &&
        password.length >= AuthViewModel.MIN_PASSWORD_LENGTH &&
        passwordsMatch

    val emailError = email.isNotBlank() && !AuthViewModel.isValidEmail(email)
    val passwordError = password.isNotBlank() && password.length < AuthViewModel.MIN_PASSWORD_LENGTH
    val confirmError = confirmPassword.isNotBlank() && password != confirmPassword

    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.Success -> onSignupSuccess()
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
                text = "Create account",
                color = colors.onBackground,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            Box(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First name") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Words,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last name") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Words,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

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
                isError = passwordError,
                supportingText = if (passwordError) {
                    { Text("Must be 8+ characters") }
                } else {
                    null
                },
                shape = RoundedCornerShape(8.dp),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm password") },
                singleLine = true,
                isError = confirmError,
                supportingText = if (confirmError) {
                    { Text("Passwords do not match") }
                } else {
                    null
                },
                shape = RoundedCornerShape(8.dp),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = { viewModel.signUp(firstName, lastName, email, password, confirmPassword) },
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
                    Text("Create account", fontWeight = FontWeight.SemiBold)
                }
            }

            TextButton(
                onClick = onNavigateToLogin,
                enabled = !isLoading,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.secondary),
            ) {
                Text("Already have an account? Log in")
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
                    text = "Sign up failed",
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
 * 1. What: Design-time preview of the Sign-up screen with empty navigation callbacks.
 * 2. Who: Called by Android Studio's Compose preview renderer.
 * 3. When: Rendered at design time in the IDE; never runs in the shipped app.
 */
@Preview(showBackground = true)
@Composable
private fun SignupScreenPreview() {
    MyFitTheme {
        SignupScreen(onNavigateToLogin = {}, onSignupSuccess = {})
    }
}
