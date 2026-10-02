package com.wandr.android.ui.common

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.wandr.android.R
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.presentation.imagecrop.CoverImageSpec

internal val CoverImageShape = RoundedCornerShape(16.dp)

/**
 * 4:3 cover image (or a brand gradient placeholder), used for groups and challenges. Display only; see
 * [CoverImagePicker] for the tappable variant. [isBusy] dims the image and shows a progress indicator.
 */
@Composable
fun CoverImage(
    coverUrl: String?,
    modifier: Modifier = Modifier,
    isBusy: Boolean = false
) {
    var isLoading by remember { mutableStateOf(isBusy) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(CoverImageSpec.ASPECT_RATIO)
            .clip(CoverImageShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                )
            )
    ) {
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = stringResource(R.string.team_cover_content_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Preview(name = "Placeholder", showBackground = true, widthDp = 360)
@Preview(
    name = "Placeholder Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = 360
)
@Preview(name = "Tablet", showBackground = true, widthDp = 840)
@Composable
private fun CoverImagePreview() {
    WandrTheme { CoverImage(coverUrl = null, modifier = Modifier.padding(16.dp)) }
}

@Preview(name = "Busy", showBackground = true, widthDp = 360)
@Composable
private fun CoverImageBusyPreview() {
    WandrTheme { CoverImage(coverUrl = null, isBusy = true, modifier = Modifier.padding(16.dp)) }
}
