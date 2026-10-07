package com.wandr.android.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.presentation.imagecrop.AvatarImageSpec

/**
 * Round avatar with its whole edit flow: source selection (library / camera / remove) -> 1:1 crop ->
 * [onAvatarReady] with a JPEG already scaled to [AvatarImageSpec.MAX_EDGE_PX]. Used for users and teams.
 */
@Composable
fun AvatarEditor(
    avatarUrl: String?,
    displayName: String,
    isBusy: Boolean,
    onAvatarReady: (ByteArray) -> Unit,
    onRemoveAvatar: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 64.dp,
    title: String = stringResource(R.string.avatar_change_title)
) {
    val flow = rememberImagePickerFlow(
        title = title,
        aspectRatio = AvatarImageSpec.ASPECT_RATIO,
        outputMaxEdgePx = AvatarImageSpec.MAX_EDGE_PX,
        jpegQuality = AvatarImageSpec.JPEG_QUALITY,
        canRemove = avatarUrl != null,
        onImageReady = onAvatarReady,
        onRemove = onRemoveAvatar
    )
    AvatarImagePicker(
        avatarUrl = avatarUrl,
        displayName = displayName,
        onPickAvatar = flow::open,
        isBusy = isBusy,
        enabled = enabled,
        size = size,
        modifier = modifier
    )
}
