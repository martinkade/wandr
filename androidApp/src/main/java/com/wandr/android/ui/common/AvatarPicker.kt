package com.wandr.android.ui.common

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.wandr.android.ui.theme.WandrTheme

@Composable
fun AvatarPicker(
    avatarUrl: String?,
    displayName: String,
    onPickAvatar: () -> Unit,
    modifier: Modifier = Modifier,
    isBusy: Boolean = false,
    enabled: Boolean = true
) = AvatarImage(
    avatarUrl = avatarUrl,
    displayName = displayName,
    modifier = modifier.clickable(enabled = enabled, onClick = onPickAvatar),
    isBusy = isBusy,
)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Busy", showBackground = true)
@Composable
private fun AvatarPickerPreview() {
    WandrTheme {
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
    WandrTheme {
        AvatarPicker(
            avatarUrl = null,
            displayName = "Martin Kade",
            onPickAvatar = {},
            isBusy = true
        )
    }
}
