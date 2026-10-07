package com.wandr.android.ui.challenge

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.profile.components.TeamMembershipItem
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.model.Team

/** Bottom sheet to choose which of their teams the user enrolls in a group challenge. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnrollTeamSheet(
    teams: List<Team>,
    /** The user's highest-priority team, marked like in the profile. */
    primaryTeamId: String?,
    onTeamSelected: (Team) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        EnrollTeamSheetContent(teams, primaryTeamId, onTeamSelected)
    }
}

@Composable
internal fun EnrollTeamSheetContent(
    teams: List<Team>,
    primaryTeamId: String?,
    onTeamSelected: (Team) -> Unit
) {
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = stringResource(R.string.challenge_enroll_dialog_title),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = stringResource(R.string.challenge_enroll_dialog_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 14.dp)
        )
        teams.forEachIndexed { index, team ->
            TeamMembershipItem(
                team = team,
                isPrimary = team.id == primaryTeamId,
                isFirstItemInSection = index == 0,
                isLastItemInSection = index == teams.lastIndex,
                dragHandleModifier = null,
                onClick = { onTeamSelected(team) }
            )
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Composable
private fun EnrollTeamSheetPreview() {
    WandrTheme {
        EnrollTeamSheetContent(
            teams = listOf(
                Team("t1", "Alpine Trail Blazers", null, null, null, "X7K9P2W1", "u1", 0L, 0L),
                Team("t2", "City Runners", null, null, null, "Q3M8D5LA", "u1", 0L, 0L)
            ),
            primaryTeamId = "t1",
            onTeamSelected = {}
        )
    }
}
