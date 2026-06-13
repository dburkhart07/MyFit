package com.example.myfit.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myfit.ui.home.HomeScreen
import com.example.myfit.ui.login.LoginScreen
import com.example.myfit.ui.login.SignupScreen
import com.example.myfit.ui.onboarding.OnboardingScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    // If a session already exists, skip straight to Home (session persistence).
    val startDestination: Any = if (FirebaseAuth.getInstance().currentUser != null) Home else Login

    NavHost(navController = navController, startDestination = startDestination) {

        composable<Login> {
            LoginScreen(
                onNavigateToSignup = { navController.navigate(Signup) },
                onLoginSuccess = {
                    navController.navigate(Home) { popUpTo<Login> { inclusive = true } }
                },
            )
        }

        composable<Signup> {
            SignupScreen(
                onNavigateToLogin = { navController.popBackStack() },
                // New accounts go through workout onboarding before reaching home.
                onSignupSuccess = {
                    navController.navigate(Onboarding) { popUpTo<Login> { inclusive = true } }
                },
            )
        }

        composable<Onboarding> {
            OnboardingScreen(
                onSubmit = {
                    navController.navigate(Home) { popUpTo<Onboarding> { inclusive = true } }
                },
            )
        }

        composable<Home> {
            HomeScreen(
                onLogout = {
                    navController.navigate(Login) { popUpTo(0) { inclusive = true } }
                },
            )
        }
    }
}
