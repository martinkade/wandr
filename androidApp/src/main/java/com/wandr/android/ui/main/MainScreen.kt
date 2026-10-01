package com.wandr.android.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.android.R
import com.wandr.android.ui.activity.ActivityHistoryScreen
import com.wandr.android.ui.challenge.ChallengeListScreen
import com.wandr.android.ui.profile.ProfileScreen
import com.wandr.android.ui.team.CreateTeamScreen
import com.wandr.android.ui.team.TeamDetailsScreen
import com.wandr.android.ui.team.TeamListScreen
import com.wandr.presentation.activity.ActivityIntent
import com.wandr.presentation.activity.ActivityState
import com.wandr.presentation.challenge.ChallengeIntent
import com.wandr.presentation.challenge.ChallengeState
import com.wandr.presentation.main.MainState
import com.wandr.presentation.profile.ProfileIntent
import com.wandr.presentation.profile.ProfileState
import com.wandr.presentation.team.TeamIntent
import com.wandr.presentation.team.TeamState
import com.wandr.presentation.teamdetails.TeamDetailsIntent
import com.wandr.presentation.teamdetails.TeamDetailsState

private enum class MainTab(val labelRes: Int, val iconRes: Int) {
    Activities(R.string.tab_activity, R.drawable.ic_tab_activity),
    Challenges(R.string.tab_challenges, R.drawable.ic_tab_challenges),
    Groups(R.string.tab_groups, R.drawable.ic_tab_groups),
    Profile(R.string.tab_profile, R.drawable.ic_tab_profile)
}

@Composable
fun MainScreen(
    mainState: MainState,
    activityState: ActivityState,
    challengeState: ChallengeState,
    profileState: ProfileState,
    teamState: TeamState,
    teamDetailsState: TeamDetailsState,
    onActivityIntent: (ActivityIntent) -> Unit,
    onChallengeIntent: (ChallengeIntent) -> Unit,
    onProfileIntent: (ProfileIntent) -> Unit,
    onTeamIntent: (TeamIntent) -> Unit,
    onTeamDetailsIntent: (TeamDetailsIntent) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRequested by rememberSaveable { mutableStateOf(MainTab.Activities) }
    var showCreateTeam by rememberSaveable { mutableStateOf(false) }
    var selectedTeamId by rememberSaveable { mutableStateOf<String?>(null) }
    val userId = mainState.userId
    val teamId = mainState.teamId
    // The Groups tab only exists for managers; fall back if the role changes while it is selected.
    val tabs = MainTab.entries.filter { it != MainTab.Groups || mainState.isManager }
    val selectedTab = if (selectedRequested in tabs) selectedRequested else MainTab.Activities

    LaunchedEffect(userId) {
        if (userId != null) {
            onActivityIntent(ActivityIntent.LoadUserActivities(userId))
            onProfileIntent(ProfileIntent.LoadProfile(userId))
        }
    }
    LaunchedEffect(userId, mainState.isManager) {
        if (userId != null && mainState.isManager) onTeamIntent(TeamIntent.LoadUserTeams(userId))
    }
    // A successful creation closes the dialog.
    LaunchedEffect(teamState.successMessage) {
        if (showCreateTeam && teamState.successMessage != null) {
            showCreateTeam = false
            onTeamIntent(TeamIntent.ClearMessages)
        }
    }
    LaunchedEffect(teamId) {
        if (teamId != null) onChallengeIntent(ChallengeIntent.LoadTeamChallenges(teamId))
    }

    Scaffold(
        modifier = modifier,
        // Each tab screen owns its top app bar (ScreenScaffold) and handles the status bar itself.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selectedTab,
                        onClick = { selectedRequested = tab },
                        icon = { Icon(painterResource(tab.iconRes), contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            if (mainState.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                when (selectedTab) {
                    MainTab.Activities -> ActivityHistoryScreen(
                        state = activityState,
                        onIntent = onActivityIntent,
                        onSelectActivity = {}
                    )
                    MainTab.Challenges -> ChallengeListScreen(
                        challenges = challengeState.challenges,
                        onSelectChallenge = {},
                        onJoinChallenge = { challenge ->
                            if (userId != null) {
                                onChallengeIntent(ChallengeIntent.JoinChallenge(challenge.id, userId))
                            }
                        }
                    )
                    MainTab.Groups -> if (selectedTeamId != null && userId != null) {
                        // Drill-down inside the tab; system back returns to the list.
                        BackHandler { selectedTeamId = null }
                        TeamDetailsScreen(
                            state = teamDetailsState,
                            onIntent = onTeamDetailsIntent,
                            onBack = { selectedTeamId = null }
                        )
                    } else {
                        TeamListScreen(
                            teams = teamState.teams,
                            onSelectTeam = { team ->
                                if (userId != null) {
                                    onTeamDetailsIntent(TeamDetailsIntent.Load(team.id, userId))
                                    selectedTeamId = team.id
                                }
                            },
                            onCreateTeam = {
                                onTeamIntent(TeamIntent.ClearMessages)
                                showCreateTeam = true
                            }
                        )
                    }
                    MainTab.Profile -> ProfileScreen(
                        state = profileState,
                        onIntent = onProfileIntent,
                        onLogout = onLogout
                    )
                }
            }

            if (showCreateTeam && userId != null) {
                Dialog(
                    onDismissRequest = { showCreateTeam = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
                ) {
                    CreateTeamScreen(
                        state = teamState,
                        onIntent = onTeamIntent,
                        creatorId = userId,
                        onCancel = { showCreateTeam = false }
                    )
                }
            }
        }
    }
}

@Preview(name = "Phone", showBackground = true, widthDp = 360, heightDp = 640)
@Preview(name = "Phone Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360, heightDp = 640)
@Preview(name = "Tablet", showBackground = true, widthDp = 840, heightDp = 1180)
@Composable
private fun MainScreenPreview() {
    WandrTheme {
        MainScreen(
            mainState = MainState(userId = "u1", teamId = null, isManager = true, isLoading = false),
            activityState = ActivityState(),
            challengeState = ChallengeState(),
            profileState = ProfileState(),
            teamState = TeamState(),
            teamDetailsState = TeamDetailsState(),
            onActivityIntent = {},
            onChallengeIntent = {},
            onProfileIntent = {},
            onTeamIntent = {},
            onTeamDetailsIntent = {},
            onLogout = {}
        )
    }
}
