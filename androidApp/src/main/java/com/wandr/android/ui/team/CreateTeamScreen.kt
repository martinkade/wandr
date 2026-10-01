package com.wandr.android.ui.team

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.android.R
import com.wandr.presentation.team.TeamIntent
import com.wandr.presentation.team.TeamState

@Composable
fun CreateTeamScreen(
    state: TeamState,
    onIntent: (TeamIntent) -> Unit,
    creatorId: String,
    modifier: Modifier = Modifier,
    onCancel: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.create_team_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.tertiary
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = state.createTeamName,
                onValueChange = { onIntent(TeamIntent.CreateTeamNameChanged(it)) },
                label = { Text(stringResource(R.string.team_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.createTeamDescription,
                onValueChange = { onIntent(TeamIntent.CreateTeamDescriptionChanged(it)) },
                label = { Text(stringResource(R.string.team_description_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )

            state.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            state.successMessage?.let { msg ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { onIntent(TeamIntent.SubmitCreateTeam(creatorId)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(stringResource(R.string.create_team_button))
                }
            }

            onCancel?.let {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = it, enabled = !state.isLoading) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Composable
private fun CreateTeamScreenPreview() {
    WandrTheme {
        CreateTeamScreen(
            state = TeamState(createTeamName = "Alpine Trail Blazers", createTeamDescription = "Hiking group for weekend trips."),
            onIntent = {},
            creatorId = "user_1"
        )
    }
}
