package com.wandr.android.ui.profile

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.wandr.android.R

@Composable
fun AvatarPicker(
    avatarUrl: String?,
    displayName: String,
    onPickAvatar: () -> Unit,
    modifier: Modifier = Modifier,
    isBusy: Boolean = false
) {
    val initials = displayName.take(2).uppercase().ifEmpty { "WA" }

    Box(
        modifier = modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(enabled = !isBusy) { onPickAvatar() },
        contentAlignment = Alignment.Center
    ) {
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = stringResource(R.string.avatar_content_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initials,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        if (isBusy) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(32.dp), color = Color.White, strokeWidth = 3.dp)
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Busy", showBackground = true)
@Composable
private fun AvatarPickerPreview() {
    MaterialTheme {
        AvatarPicker(
            avatarUrl = null,
            displayName = "Martin Kade",
            onPickAvatar = {},
            isBusy = false
        )
    }
}

@Preview(name = "Busy Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun AvatarPickerBusyPreview() {
    MaterialTheme { AvatarPicker(avatarUrl = null, displayName = "Martin Kade", onPickAvatar = {}, isBusy = true) }
}
