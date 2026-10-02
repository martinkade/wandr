package com.wandr.android.ui.main

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wandr.android.ui.activity.ActivityHistoryScreen
import com.wandr.android.ui.challenge.ChallengeDetailsScreen
import com.wandr.android.ui.challenge.ChallengeListScreen
import com.wandr.android.ui.common.SlideInOverlay
import com.wandr.android.ui.profile.ProfileScreen
import com.wandr.android.ui.team.GroupsScreen
import com.wandr.android.ui.team.TeamDetailsScreen
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.presentation.main.MainViewModel
import org.koin.compose.koinInject

/**
 * Post-login root: resolves the user and role, shows the tab bar and the selected tab. Every tab screen
 * injects its own view model.
 */
@Composable
fun MainScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = koinInject()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedRequested by rememberSaveable { mutableStateOf(MainTab.Activities) }
    LaunchedEffect(Unit) { viewModel.load() }

    // The Groups tab only exists for managers; fall back if the role changes while it is selected.
    val tabs = MainTab.entries.filter { it != MainTab.Groups || state.isManager }
    val selectedTab = if (selectedRequested in tabs) selectedRequested else MainTab.Activities
    val userId = state.userId
    // Group details slide in over the whole main screen, including the tab bar (like a pushed page).
    var openTeamId by rememberSaveable { mutableStateOf<String?>(null) }
    var openChallengeId by rememberSaveable { mutableStateOf<String?>(null) }

    Box(modifier = modifier) {
        MainScreenContent(
            tabs = tabs,
            selectedTab = selectedTab,
            onSelectTab = { selectedRequested = it },
            isLoading = state.isLoading
        ) { tab ->
            if (userId != null) {
                when (tab) {
                    MainTab.Activities -> ActivityHistoryScreen(userId = userId)
                    MainTab.Challenges -> ChallengeListScreen(
                        userId = userId,
                        isManager = state.isManager,
                        onOpenChallenge = { openChallengeId = it }
                    )
                    MainTab.Groups -> GroupsScreen(userId = userId, onOpenTeam = { openTeamId = it })
                    MainTab.Profile -> ProfileScreen(userId = userId, onLogout = { viewModel.logout(onLogout) })
                }
            }
        }

        SlideInOverlay(item = openTeamId.takeIf { userId != null }, onBack = { openTeamId = null }) { teamId ->
            TeamDetailsScreen(teamId = teamId, userId = userId.orEmpty(), onBack = { openTeamId = null })
        }
        SlideInOverlay(item = openChallengeId.takeIf { userId != null }, onBack = { openChallengeId = null }) { challengeId ->
            ChallengeDetailsScreen(
                challengeId = challengeId,
                userId = userId.orEmpty(),
                isManager = state.isManager,
                onBack = { openChallengeId = null }
            )
        }
    }
}

/**
 * Stateless shell of the main screen: the bottom tab bar plus a slot for the selected tab's content.
 * Each tab screen owns its top app bar, so none is drawn here.
 */
@Composable
private fun MainScreenContent(
    tabs: List<MainTab>,
    selectedTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    tabContent: @Composable (MainTab) -> Unit
) {
    Scaffold(
        modifier = modifier,
        // Each tab screen handles the status bar itself (ScreenScaffold).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selectedTab,
                        onClick = { onSelectTab(tab) },
                        icon = { Icon(painterResource(tab.iconRes), contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            if (isLoading) CircularProgressIndicator(Modifier.align(Alignment.Center)) else tabContent(selectedTab)
        }
    }
}

@Preview(name = "Manager", showBackground = true)
@Preview(name = "Manager Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Manager Tablet", widthDp = 840, heightDp = 600, showBackground = true)
@Composable
private fun MainContentManagerPreview() {
    WandrTheme {
        MainScreenContent(
            tabs = MainTab.entries, selectedTab = MainTab.Groups, onSelectTab = {}, isLoading = false
        ) { tab -> Text(tab.name) }
    }
}

@Preview(name = "Member", showBackground = true)
@Preview(name = "Loading Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun MainContentMemberPreview() {
    WandrTheme {
        MainScreenContent(
            tabs = MainTab.entries - MainTab.Groups, selectedTab = MainTab.Activities, onSelectTab = {}, isLoading = false
        ) { tab -> Text(tab.name) }
    }
}
