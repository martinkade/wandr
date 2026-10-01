package com.wandr.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.wandr.android.ui.auth.LoginScreen
import com.wandr.android.ui.auth.RegisterScreen
import com.wandr.android.ui.dashboard.DashboardScreen
import com.wandr.android.ui.startup.InitScreen
import com.wandr.presentation.activity.ActivityViewModel
import com.wandr.presentation.auth.LoginViewModel
import com.wandr.presentation.challenge.ChallengeViewModel
import com.wandr.presentation.dashboard.DashboardViewModel
import com.wandr.presentation.navigation.DashboardRoute
import com.wandr.presentation.navigation.InitRoute
import com.wandr.presentation.navigation.LoginRoute
import com.wandr.presentation.navigation.RegisterRoute
import com.wandr.presentation.navigation.WandrNavigator
import com.wandr.presentation.navigation.wandrNavConfig
import com.wandr.presentation.profile.ProfileViewModel
import com.wandr.presentation.startup.StartupIntent
import com.wandr.presentation.startup.StartupState
import com.wandr.presentation.startup.StartupViewModel
import org.koin.compose.koinInject

/** Root of the app: Init → (Login | Dashboard). */
@Composable
fun WandrApp() {
    val backStack = rememberNavBackStack(wandrNavConfig, InitRoute)
    val navigator = remember(backStack) { WandrNavigator(backStack) }

    NavDisplay(
        backStack = backStack,
        onBack = { navigator.popBackStack() },
        entryProvider = entryProvider {
            entry<InitRoute> {
                val viewModel = koinInject<StartupViewModel>()
                val state by viewModel.uiState.collectAsState()
                LaunchedEffect(Unit) { viewModel.processIntent(StartupIntent.Start) }
                LaunchedEffect(state) {
                    when (state) {
                        StartupState.Authenticated -> navigator.replaceRoot(DashboardRoute)
                        StartupState.Unauthenticated -> navigator.replaceRoot(LoginRoute)
                        else -> Unit
                    }
                }
                InitScreen(state = state, onRetry = { viewModel.processIntent(StartupIntent.Retry) })
            }
            entry<LoginRoute> {
                val viewModel = koinInject<LoginViewModel>()
                val state by viewModel.uiState.collectAsState()
                LaunchedEffect(state.isAuthenticated) {
                    if (state.isAuthenticated) navigator.replaceRoot(DashboardRoute)
                }
                LoginScreen(
                    state = state,
                    onIntent = viewModel::processIntent,
                    onNavigateToRegister = { navigator.navigateTo(RegisterRoute) }
                )
            }
            entry<RegisterRoute> {
                val viewModel = koinInject<LoginViewModel>()
                val state by viewModel.uiState.collectAsState()
                LaunchedEffect(state.isAuthenticated) {
                    if (state.isAuthenticated) navigator.replaceRoot(DashboardRoute)
                }
                RegisterScreen(
                    state = state,
                    onIntent = viewModel::processIntent,
                    onNavigateToLogin = { navigator.popBackStack() }
                )
            }
            entry<DashboardRoute> {
                val dashboardViewModel = koinInject<DashboardViewModel>()
                val activityViewModel = koinInject<ActivityViewModel>()
                val challengeViewModel = koinInject<ChallengeViewModel>()
                val profileViewModel = koinInject<ProfileViewModel>()
                LaunchedEffect(Unit) { dashboardViewModel.load() }
                DashboardScreen(
                    dashboardState = dashboardViewModel.uiState.collectAsState().value,
                    activityState = activityViewModel.state.collectAsState().value,
                    challengeState = challengeViewModel.uiState.collectAsState().value,
                    profileState = profileViewModel.uiState.collectAsState().value,
                    onActivityIntent = activityViewModel::processIntent,
                    onChallengeIntent = challengeViewModel::processIntent,
                    onProfileIntent = profileViewModel::processIntent,
                    onLogout = { dashboardViewModel.logout { navigator.replaceRoot(LoginRoute) } }
                )
            }
        }
    )
}
