package com.wandr.android.ui.common

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wandr.android.ui.theme.WandrTheme

@Composable
fun AvatarImagePicker(
    avatarUrl: String?,
    displayName: String,
    onPickAvatar: () -> Unit,
    modifier: Modifier = Modifier,
    isBusy: Boolean = false,
    enabled: Boolean = true,
    size: Dp = 64.dp
) = AvatarImage(
    avatarUrl = avatarUrl,
    displayName = displayName,
    modifier = modifier.clickable(enabled = enabled, onClick = onPickAvatar),
    isBusy = isBusy,
    size = size,
)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Busy", showBackground = true)
@Composable
private fun AvatarImagePickerPreview() {
    WandrTheme {
        AvatarImagePicker(
            avatarUrl = null,
            displayName = "Martin Kade",
            onPickAvatar = {},
            isBusy = false
        )
    }
}

@Preview(name = "Busy Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun AvatarImagePickerBusyPreview() {
    WandrTheme {
        AvatarImagePicker(
            avatarUrl = null,
            displayName = "Martin Kade",
            onPickAvatar = {},
            isBusy = true
        )
    }
}
