package com.wandr.android.ui.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.android.R
import com.wandr.android.ui.activity.ActivityHistoryScreen
import com.wandr.android.ui.challenge.ChallengeListScreen
import com.wandr.android.ui.profile.ProfileScreen
import com.wandr.presentation.activity.ActivityIntent
import com.wandr.presentation.activity.ActivityState
import com.wandr.presentation.challenge.ChallengeIntent
import com.wandr.presentation.challenge.ChallengeState
import com.wandr.presentation.dashboard.DashboardState
import com.wandr.presentation.profile.ProfileIntent
import com.wandr.presentation.profile.ProfileState

private enum class DashboardTab(val labelRes: Int, val iconRes: Int) {
    Activities(R.string.tab_activity, R.drawable.ic_tab_activity),
    Challenges(R.string.tab_challenges, R.drawable.ic_tab_challenges),
    Profile(R.string.tab_profile, R.drawable.ic_tab_profile)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    dashboardState: DashboardState,
    activityState: ActivityState,
    challengeState: ChallengeState,
    profileState: ProfileState,
    onActivityIntent: (ActivityIntent) -> Unit,
    onChallengeIntent: (ChallengeIntent) -> Unit,
    onProfileIntent: (ProfileIntent) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(DashboardTab.Activities) }
    val userId = dashboardState.userId
    val teamId = dashboardState.teamId

    LaunchedEffect(userId) {
        if (userId != null) {
            onActivityIntent(ActivityIntent.LoadUserActivities(userId))
            onProfileIntent(ProfileIntent.LoadProfile(userId))
        }
    }
    LaunchedEffect(teamId) {
        if (teamId != null) onChallengeIntent(ChallengeIntent.LoadTeamChallenges(teamId))
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(selectedTab.labelRes)) },
                actions = {
                    if (selectedTab == DashboardTab.Profile) {
                        TextButton(onClick = onLogout) { Text(stringResource(R.string.logout_button)) }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                DashboardTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selectedTab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(painterResource(tab.iconRes), contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (dashboardState.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                when (selectedTab) {
                    DashboardTab.Activities -> ActivityHistoryScreen(
                        state = activityState,
                        onIntent = onActivityIntent,
                        onSelectActivity = {}
                    )
                    DashboardTab.Challenges -> ChallengeListScreen(
                        challenges = challengeState.challenges,
                        onSelectChallenge = {},
                        onJoinChallenge = { challenge ->
                            if (userId != null) {
                                onChallengeIntent(ChallengeIntent.JoinChallenge(challenge.id, userId))
                            }
                        }
                    )
                    DashboardTab.Profile -> ProfileScreen(
                        state = profileState,
                        onIntent = onProfileIntent
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
private fun DashboardScreenPreview() {
    WandrTheme {
        DashboardScreen(
            dashboardState = DashboardState(userId = "u1", teamId = null, isLoading = false),
            activityState = ActivityState(),
            challengeState = ChallengeState(),
            profileState = ProfileState(),
            onActivityIntent = {},
            onChallengeIntent = {},
            onProfileIntent = {},
            onLogout = {}
        )
    }
}
