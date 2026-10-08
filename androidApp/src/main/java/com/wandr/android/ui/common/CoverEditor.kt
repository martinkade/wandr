package com.wandr.android.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wandr.android.R
import com.wandr.presentation.imagecrop.CoverImageSpec

/**
 * Cover image with its whole edit flow: source selection (library / camera / remove) -> 4:3 crop ->
 * [onCoverReady] with a JPEG already scaled to [CoverImageSpec.MAX_EDGE_PX]. Used for teams and challenges.
 */
@Composable
fun CoverEditor(
    coverUrl: String?,
    isBusy: Boolean,
    onCoverReady: (ByteArray) -> Unit,
    onRemoveCover: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    title: String = stringResource(R.string.team_cover_title)
) {
    val flow = rememberImagePickerFlow(
        title = title,
        aspectRatio = CoverImageSpec.ASPECT_RATIO,
        outputMaxEdgePx = CoverImageSpec.MAX_EDGE_PX,
        jpegQuality = CoverImageSpec.JPEG_QUALITY,
        canRemove = coverUrl != null,
        onImageReady = onCoverReady,
        onRemove = onRemoveCover
    )
    CoverImagePicker(
        coverUrl = coverUrl,
        onPickCover = flow::open,
        isBusy = isBusy,
        enabled = enabled,
        modifier = modifier
    )
}
