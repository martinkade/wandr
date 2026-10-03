package com.wandr.android.ui.profile.components

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wandr.android.R
import com.wandr.presentation.team.TeamIntent
import com.wandr.presentation.team.TeamViewModel
import org.koin.compose.koinInject

/**
 * The user's groups in the profile: reorder them by priority and join another one by invite code or QR code. Only the
 * first group counts for group challenges.
 */
@Composable
fun TeamMembershipsSection(
    userId: String,
    modifier: Modifier = Modifier,
    viewModel: TeamViewModel = koinInject()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showJoin by rememberSaveable { mutableStateOf(false) }
    val joinedText = stringResource(R.string.join_team_success)
    val scanFailedText = stringResource(R.string.join_team_scan_failed)
    LaunchedEffect(userId) { viewModel.processIntent(TeamIntent.LoadUserTeams(userId)) }

    LaunchedEffect(state.successMessage, state.errorMessage) {
        when {
            state.successMessage != null -> {
                showJoin = false
                Toast.makeText(context, joinedText, Toast.LENGTH_SHORT).show()
                viewModel.processIntent(TeamIntent.ClearMessages)
            }
            // While the dialog is open it shows the error itself; a rejected reorder is reported here.
            state.errorMessage != null && !showJoin -> {
                Toast.makeText(context, state.errorMessage, Toast.LENGTH_LONG).show()
                viewModel.processIntent(TeamIntent.ClearMessages)
            }
        }
    }

    TeamMembershipList(
        teams = state.teams,
        onReorder = { ordered -> viewModel.processIntent(TeamIntent.ReorderTeams(userId, ordered.map { it.id })) },
        onJoin = {
            viewModel.processIntent(TeamIntent.ClearMessages)
            showJoin = true
        },
        modifier = modifier
    )

    if (showJoin) {
        JoinTeamDialog(
            code = state.joinInviteCode,
            onCodeChange = { viewModel.processIntent(TeamIntent.JoinInviteCodeChanged(it)) },
            isJoining = state.isLoading,
            errorMessage = state.errorMessage,
            onScan = {
                scanInviteQr(
                    context = context,
                    onScanned = { raw ->
                        // The QR code holds wandr://invite/{code}; the use case unwraps it.
                        viewModel.processIntent(TeamIntent.JoinInviteCodeChanged(raw))
                        viewModel.processIntent(TeamIntent.SubmitJoinTeam(userId))
                    },
                    onFailed = { Toast.makeText(context, scanFailedText, Toast.LENGTH_LONG).show() }
                )
            },
            onJoin = { viewModel.processIntent(TeamIntent.SubmitJoinTeam(userId)) },
            onDismiss = {
                showJoin = false
                viewModel.processIntent(TeamIntent.JoinInviteCodeChanged(""))
                viewModel.processIntent(TeamIntent.ClearMessages)
            }
        )
    }
}
