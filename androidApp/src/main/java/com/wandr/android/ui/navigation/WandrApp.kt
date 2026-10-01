package com.wandr.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.wandr.android.ui.auth.LoginScreen
import com.wandr.android.ui.auth.RegisterScreen
import com.wandr.android.ui.main.MainScreen
import com.wandr.android.ui.startup.InitScreen
import com.wandr.presentation.navigation.InitRoute
import com.wandr.presentation.navigation.LoginRoute
import com.wandr.presentation.navigation.MainRoute
import com.wandr.presentation.navigation.RegisterRoute
import com.wandr.presentation.navigation.WandrNavigator
import com.wandr.presentation.navigation.wandrNavConfig

/** Root of the app: Init → (Login | Main). Screens inject their own view models; this only wires routes. */
@Composable
fun WandrApp() {
    val backStack = rememberNavBackStack(wandrNavConfig, InitRoute)
    val navigator = remember(backStack) { WandrNavigator(backStack) }

    NavDisplay(
        backStack = backStack,
        onBack = { navigator.popBackStack() },
        entryProvider = entryProvider {
            entry<InitRoute> {
                InitScreen(
                    onAuthenticated = { navigator.replaceRoot(MainRoute) },
                    onUnauthenticated = { navigator.replaceRoot(LoginRoute) }
                )
            }
            entry<LoginRoute> {
                LoginScreen(
                    onAuthenticated = { navigator.replaceRoot(MainRoute) },
                    onNavigateToRegister = { navigator.navigateTo(RegisterRoute) }
                )
            }
            entry<RegisterRoute> {
                RegisterScreen(
                    onAuthenticated = { navigator.replaceRoot(MainRoute) },
                    onNavigateToLogin = { navigator.popBackStack() }
                )
            }
            entry<MainRoute> {
                MainScreen(onLogout = { navigator.replaceRoot(LoginRoute) })
            }
        }
    )
}
