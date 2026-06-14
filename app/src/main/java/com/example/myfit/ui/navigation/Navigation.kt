package com.example.myfit.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myfit.ui.account.AccountScreen
import com.example.myfit.ui.components.BottomTab
import com.example.myfit.ui.history.HistoryScreen
import com.example.myfit.ui.home.HomeScreen
import com.example.myfit.ui.login.LoginScreen
import com.example.myfit.ui.login.SignupScreen
import com.example.myfit.ui.onboarding.OnboardingScreen
import com.google.firebase.auth.FirebaseAuth
import com.example.myfit.ui.workouts.WorkoutsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    // If a session already exists, skip straight to Home (session persistence).
    val startDestination: Any = if (FirebaseAuth.getInstance().currentUser != null) Home else Login

    // Shared by every main screen's top-bar logout: clear the whole stack back to Login.
    // (The Firebase sign-out itself happens in MyFitTopBar via AuthViewModel.)
    val onLogout: () -> Unit = {
        navController.navigate(Login) { popUpTo(0) { inclusive = true } }
    }

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
            WorkoutsScreen(
                onLogout = onLogout,
                onSelectTab = navController::selectTab,
            )
        }

        composable<History> {
            HistoryScreen(
                onLogout = onLogout,
                onSelectTab = navController::selectTab,
            )
        }

        composable<Account> {
            AccountScreen(
                onLogout = onLogout,
                onSelectTab = navController::selectTab,
            )
        }
    }
}

/**
 * 1. What: Switches to the destination backing [tab], keeping a single instance of each tab.
 * 2. Who: Passed to every main screen's bottom bar as its onSelect handler.
 * 3. When: Fires when the user taps a tab; Home anchors the back stack so tabs don't pile up.
 */
private fun NavHostController.selectTab(tab: BottomTab) {
    val destination: Any = when (tab) {
        BottomTab.Workouts -> Home
        BottomTab.History -> History
        BottomTab.Account -> Account
    }
    navigate(destination) {
        // Keep Home as the back-stack anchor; saving/restoring state keeps each tab's scroll etc.
        popUpTo(Home) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
