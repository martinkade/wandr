package com.wandr.android.ui.challenge

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wandr.android.R
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.model.Team

/** Lets the user choose which of their teams to enroll in a group challenge. */
@Composable
fun EnrollTeamDialog(
    teams: List<Team>,
    onTeamSelected: (Team) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.challenge_enroll_dialog_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.challenge_enroll_dialog_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                teams.forEach { team ->
                    TextButton(onClick = { onTeamSelected(team) }) { Text(team.name) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_button)) } }
    )
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Composable
private fun EnrollTeamDialogPreview() {
    WandrTheme {
        EnrollTeamDialog(
            teams = listOf(
                Team("t1", "Alpine Trail Blazers", null, null, null, "X7K9P2W1", "u1", 0L, 0L),
                Team("t2", "City Runners", null, null, null, "Q3M8D5LA", "u1", 0L, 0L)
            ),
            onTeamSelected = {}, onDismiss = {}
        )
    }
}
