package com.wandr.android.ui.challenge

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.model.ChallengeAction

/** The participation action of a challenge: filled for joining / enrolling, outlined for leaving / withdrawing. */
@Composable
fun ChallengeActionButton(
    action: ChallengeAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = stringResource(
        when (action) {
            ChallengeAction.JOIN -> R.string.join_challenge_button
            ChallengeAction.LEAVE -> R.string.leave_challenge_button
            ChallengeAction.ENROLL_TEAM -> R.string.challenge_enroll_team_button
            ChallengeAction.WITHDRAW_TEAM -> R.string.challenge_withdraw_team_button
        }
    )
    when (action) {
        ChallengeAction.JOIN, ChallengeAction.ENROLL_TEAM -> Button(onClick = onClick, modifier = modifier) { Text(label) }
        ChallengeAction.LEAVE, ChallengeAction.WITHDRAW_TEAM -> OutlinedButton(onClick = onClick, modifier = modifier) { Text(label) }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Composable
private fun ChallengeActionButtonPreview() {
    WandrTheme {
        Column(Modifier.padding(16.dp)) {
            ChallengeAction.entries.forEach { ChallengeActionButton(it, onClick = {}, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) }
        }
    }
}
