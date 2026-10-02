package com.wandr.android.ui.common

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/**
 * 4:3 cover image (or a brand gradient placeholder), used for groups and challenges. When [isEditable] it is
 * tappable and shows a "Change cover" hint; the caller opens the image picker in [onClick].
 */
@Composable
fun CoverImage(
    coverUrl: String?,
    isEditable: Boolean,
    isBusy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(CoverImageSpec.ASPECT_RATIO)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                )
            )
            .clickable(enabled = isEditable && !isBusy, onClick = onClick)
    ) {
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = stringResource(R.string.team_cover_content_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (isEditable && !isBusy) {
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
        if (isBusy) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
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
@Preview(name = "Editing Tablet", showBackground = true, widthDp = 840)
@Composable
private fun CoverImagePreview() {
    WandrTheme {
        CoverImage(
            coverUrl = null,
            isEditable = true,
            isBusy = false,
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Busy", showBackground = true, widthDp = 360)
@Composable
private fun CoverImageBusyPreview() {
    WandrTheme {
        CoverImage(
            coverUrl = null,
            isEditable = true,
            isBusy = true,
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
