package com.wandr.android.ui.team

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/** Groups the signed-in manager belongs to, with a button to create a new one. */
@Composable
fun TeamListScreen(
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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
private fun TeamListScreenPreview() {
    WandrTheme { TeamListScreen(teams = sampleTeams(), onSelectTeam = {}, onCreateTeam = {}) }
}

@Preview(name = "Empty", showBackground = true)
@Preview(name = "Empty Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TeamListScreenEmptyPreview() {
    WandrTheme { TeamListScreen(teams = emptyList(), onSelectTeam = {}, onCreateTeam = {}) }
}
