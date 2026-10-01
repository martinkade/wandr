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
import com.wandr.android.ui.main.MainScreen
import com.wandr.android.ui.startup.InitScreen
import com.wandr.presentation.activity.ActivityViewModel
import com.wandr.presentation.auth.LoginViewModel
import com.wandr.presentation.challenge.ChallengeViewModel
import com.wandr.presentation.main.MainViewModel
import com.wandr.presentation.navigation.MainRoute
import com.wandr.presentation.navigation.InitRoute
import com.wandr.presentation.navigation.LoginRoute
import com.wandr.presentation.navigation.RegisterRoute
import com.wandr.presentation.navigation.WandrNavigator
import com.wandr.presentation.navigation.wandrNavConfig
import com.wandr.presentation.profile.ProfileViewModel
import com.wandr.presentation.startup.StartupIntent
import com.wandr.presentation.startup.StartupState
import com.wandr.presentation.startup.StartupViewModel
import com.wandr.presentation.team.TeamViewModel
import com.wandr.presentation.teamdetails.TeamDetailsViewModel
import org.koin.compose.koinInject

/** Root of the app: Init → (Login | Main). */
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
                        StartupState.Authenticated -> navigator.replaceRoot(MainRoute)
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
                    if (state.isAuthenticated) navigator.replaceRoot(MainRoute)
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
                    if (state.isAuthenticated) navigator.replaceRoot(MainRoute)
                }
                RegisterScreen(
                    state = state,
                    onIntent = viewModel::processIntent,
                    onNavigateToLogin = { navigator.popBackStack() }
                )
            }
            entry<MainRoute> {
                val mainViewModel = koinInject<MainViewModel>()
                val activityViewModel = koinInject<ActivityViewModel>()
                val challengeViewModel = koinInject<ChallengeViewModel>()
                val profileViewModel = koinInject<ProfileViewModel>()
                val teamViewModel = koinInject<TeamViewModel>()
                val teamDetailsViewModel = koinInject<TeamDetailsViewModel>()
                LaunchedEffect(Unit) { mainViewModel.load() }
                MainScreen(
                    mainState = mainViewModel.uiState.collectAsState().value,
                    activityState = activityViewModel.state.collectAsState().value,
                    challengeState = challengeViewModel.uiState.collectAsState().value,
                    profileState = profileViewModel.uiState.collectAsState().value,
                    teamState = teamViewModel.uiState.collectAsState().value,
                    teamDetailsState = teamDetailsViewModel.uiState.collectAsState().value,
                    onActivityIntent = activityViewModel::processIntent,
                    onChallengeIntent = challengeViewModel::processIntent,
                    onProfileIntent = profileViewModel::processIntent,
                    onTeamIntent = teamViewModel::processIntent,
                    onTeamDetailsIntent = teamDetailsViewModel::processIntent,
                    onLogout = { mainViewModel.logout { navigator.replaceRoot(LoginRoute) } }
                )
            }
        }
    )
}
