package com.wandr.android.ui.profile.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.model.Profile

@Composable
fun ProfileHeaderRow(
    profile: Profile,
    modifier: Modifier = Modifier
) = Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    AvatarImage(
        avatarUrl = profile.avatarUrl,
        displayName = profile.displayName,
    )
    Column(modifier = Modifier.weight(1.0f)) {
        Text(
            text = profile.displayName,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = profile.bio ?: "-",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun previewProfile(displayName: String = "Martin Kade", bio: String? = "Outdoor hiker & developer.") = Profile(
    id = "1", username = "martinkade", displayName = displayName, avatarUrl = null,
    bio = bio, createdAt = 1_768_435_200_000L, updatedAt = 0L
)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Preview(name = "Tablet", widthDp = 840, showBackground = true)
@Composable
private fun ProfileHeaderRowPreview() {
    WandrTheme { ProfileHeaderRow(profile = previewProfile(), modifier = Modifier.padding(16.dp)) }
}

@Preview(name = "No bio", showBackground = true)
@Preview(name = "No bio Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun ProfileHeaderRowNoBioPreview() {
    WandrTheme { ProfileHeaderRow(profile = previewProfile(bio = null), modifier = Modifier.padding(16.dp)) }
}

@Preview(name = "Long text (ellipsis)", showBackground = true, widthDp = 320)
@Composable
private fun ProfileHeaderRowLongTextPreview() {
    WandrTheme {
        ProfileHeaderRow(
            profile = previewProfile(
                displayName = "Maximilian Alexander von Hohenzollern-Sigmaringen",
                bio = "Outdoor hiker, trail runner, cyclist and developer. Loves long weekend trips through the Alps, " +
                    "early morning summit starts and strong coffee at the hut afterwards."
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
