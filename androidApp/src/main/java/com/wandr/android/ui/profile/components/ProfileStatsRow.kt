package com.wandr.android.ui.profile.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.theme.WandrTheme
import java.text.NumberFormat

/**
 * Activity count and "active since" date next to the edit button.
 *
 * @param memberSince already formatted date (see `AppDateFormatter`), or null when unknown
 */
@Composable
fun ProfileStatsRow(
    activityCount: Int,
    memberSince: String?,
    onEnterEditMode: () -> Unit,
    modifier: Modifier = Modifier
) = Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(24.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    val locale = LocalConfiguration.current.locales[0]
    val formattedCount = remember(activityCount, locale) {
        NumberFormat.getIntegerInstance(locale).format(activityCount)
    }

    Column {
        Text(
            text = stringResource(R.string.profile_stats_member_since),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = memberSince ?: stringResource(R.string.empty_value_placeholder),
            style = MaterialTheme.typography.titleMedium
        )
    }
    Column {
        Text(
            text = stringResource(R.string.profile_stats_activities),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = formattedCount,
            style = MaterialTheme.typography.titleMedium
        )
    }
    Spacer(Modifier.weight(1.0f))
    OutlinedButton(onClick = onEnterEditMode) {
        Text(
            text = stringResource(R.string.profile_edit_button),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Composable
private fun ProfileStatsRowPreview() {
    WandrTheme {
        ProfileStatsRow(
            activityCount = 1234,
            memberSince = "Oct 1, 2026",
            onEnterEditMode = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Unknown date", showBackground = true)
@Composable
private fun ProfileStatsRowUnknownPreview() {
    WandrTheme {
        ProfileStatsRow(
            activityCount = 0,
            memberSince = null,
            onEnterEditMode = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
