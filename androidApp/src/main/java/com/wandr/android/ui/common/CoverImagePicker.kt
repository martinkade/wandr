package com.wandr.android.ui.common

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.theme.WandrTheme

/**
 * A [CoverImage] that can be tapped to pick a new cover ([onPickCover]). While [enabled] it shows a "Change cover"
 * hint; when not enabled (e.g. the user may not edit) it behaves like a plain [CoverImage].
 */
@Composable
fun CoverImagePicker(
    coverUrl: String?,
    onPickCover: () -> Unit,
    modifier: Modifier = Modifier,
    isBusy: Boolean = false,
    enabled: Boolean = true
) {
    val tappable = enabled && !isBusy
    Box(
        modifier = modifier
            .clip(CoverImageShape)
            .clickable(enabled = tappable, onClick = onPickCover)
    ) {
        CoverImage(coverUrl = coverUrl, isBusy = isBusy)
        if (tappable) {
            Text(
                text = stringResource(R.string.team_cover_change),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Preview(name = "Light Mode", showBackground = true, widthDp = 360)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360)
@Preview(name = "Tablet", showBackground = true, widthDp = 840)
@Composable
private fun CoverImagePickerPreview() {
    WandrTheme { CoverImagePicker(coverUrl = null, onPickCover = {}, modifier = Modifier.padding(16.dp)) }
}

@Preview(name = "Disabled (no hint)", showBackground = true, widthDp = 360)
@Composable
private fun CoverImagePickerDisabledPreview() {
    WandrTheme {
        CoverImagePicker(coverUrl = null, onPickCover = {}, enabled = false, modifier = Modifier.padding(16.dp))
    }
}

@Preview(name = "Busy Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360)
@Composable
private fun CoverImagePickerBusyPreview() {
    WandrTheme { CoverImagePicker(coverUrl = null, onPickCover = {}, isBusy = true, modifier = Modifier.padding(16.dp)) }
}
