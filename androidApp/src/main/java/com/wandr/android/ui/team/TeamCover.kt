package com.wandr.android.ui.team

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

/** 4:3 cover image (or a brand gradient placeholder). In edit mode it is tappable to change the cover. */
@Composable
fun TeamCover(
    coverUrl: String?,
    isEditing: Boolean,
    isBusy: Boolean,
    onChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(CoverImageSpec.ASPECT_RATIO)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
                )
            )
            .clickable(enabled = isEditing && !isBusy, onClick = onChange)
    ) {
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = stringResource(R.string.team_cover_content_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (isEditing && !isBusy) {
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
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

@Preview(name = "Placeholder", showBackground = true, widthDp = 360)
@Preview(name = "Placeholder Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360)
@Preview(name = "Editing Tablet", showBackground = true, widthDp = 840)
@Composable
private fun TeamCoverPreview() {
    WandrTheme { TeamCover(coverUrl = null, isEditing = true, isBusy = false, onChange = {}, modifier = Modifier.padding(16.dp)) }
}

@Preview(name = "Busy", showBackground = true, widthDp = 360)
@Composable
private fun TeamCoverBusyPreview() {
    WandrTheme { TeamCover(coverUrl = null, isEditing = true, isBusy = true, onChange = {}, modifier = Modifier.padding(16.dp)) }
}
