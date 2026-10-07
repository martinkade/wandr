package com.wandr.android.ui.team

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.common.ScreenScaffold
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.model.Team
import com.wandr.presentation.team.TeamIntent
import com.wandr.presentation.team.TeamViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Groups tab for managers: the list and a bottom sheet to create a group (the same sheet as for editing one).
 * Opening a group is reported via [onOpenTeam]; the details page is shown by the main screen, above the tab bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    userId: String,
    onOpenTeam: (teamId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TeamViewModel = koinInject()
) {
    val state by viewModel.uiState.collectAsState()
    var showCreate by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val isSaving by rememberUpdatedState(state.isLoading)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { !isSaving } // no swipe-away while saving
    )
    LaunchedEffect(userId) { viewModel.processIntent(TeamIntent.LoadUserTeams(userId)) }

    fun resetForm() {
        viewModel.processIntent(TeamIntent.CreateTeamNameChanged(""))
        viewModel.processIntent(TeamIntent.CreateTeamDescriptionChanged(""))
        viewModel.processIntent(TeamIntent.ClearMessages)
    }

    fun closeSheet() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            showCreate = false
            resetForm()
        }
    }

    // A created group closes the sheet; errors show in the sheet.
    LaunchedEffect(state.successMessage, state.errorMessage) {
        if (showCreate && state.successMessage != null) {
            closeSheet()
        } else if (state.errorMessage != null) {
            val message = state.errorMessage ?: return@LaunchedEffect
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(message)
            }
            viewModel.processIntent(TeamIntent.ClearMessages)
        }
    }

    GroupList(
        teams = state.teams,
        onSelectTeam = { onOpenTeam(it.id) },
        onCreateTeam = { showCreate = true },
        modifier = modifier
    )

    if (showCreate) {
        ModalBottomSheet(
            onDismissRequest = {
                showCreate = false
                resetForm()
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            TeamEditScreen(
                title = stringResource(R.string.create_team_title),
                name = state.createTeamName,
                description = state.createTeamDescription,
                onNameChange = { viewModel.processIntent(TeamIntent.CreateTeamNameChanged(it)) },
                onDescriptionChange = { viewModel.processIntent(TeamIntent.CreateTeamDescriptionChanged(it)) },
                isSaving = state.isLoading,
                onSave = { viewModel.processIntent(TeamIntent.SubmitCreateTeam(userId)) },
                onCancel = ::closeSheet,
                snackbarHostState = snackbarHostState
            )
        }
    }
}

/** Groups the signed-in manager belongs to, with a button to create a new one. */
@Composable
private fun GroupList(
    teams: List<Team>,
    onSelectTeam: (Team) -> Unit,
    onCreateTeam: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenScaffold(
        title = stringResource(R.string.tab_groups),
        modifier = modifier,
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onCreateTeam) {
                Text(stringResource(R.string.groups_new_button))
            }
        }
    ) { padding ->
    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        if (teams.isEmpty()) {
            Text(
                text = stringResource(R.string.groups_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(32.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // Bottom padding keeps the last card clear of the floating button.
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(teams, key = { it.id }) { team ->
                    TeamCard(team = team, onSelect = { onSelectTeam(team) })
                }
            }
        }
    }
    }
}

private fun sampleTeams() = listOf(
    Team("t1", "Alpine Trail Blazers", "Hiking group for weekend trips.", null, null, "X7K9P2W1", "u1", 0L, 0L),
    Team("t2", "City Runners", null, null, null, "Q3M8D5LA", "u1", 0L, 0L)
)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Tablet", widthDp = 840, heightDp = 600, showBackground = true)
@Composable
private fun GroupListPreview() {
    WandrTheme { GroupList(teams = sampleTeams(), onSelectTeam = {}, onCreateTeam = {}) }
}

@Preview(name = "Empty", showBackground = true)
@Preview(name = "Empty Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun GroupListEmptyPreview() {
    WandrTheme { GroupList(teams = emptyList(), onSelectTeam = {}, onCreateTeam = {}) }
}
