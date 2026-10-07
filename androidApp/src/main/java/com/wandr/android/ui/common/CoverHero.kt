package com.wandr.android.ui.common

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.wandr.android.R
import com.wandr.android.ui.theme.WandrTheme

/**
 * The image area at the top of a challenge or group: the cover photo, or without one a gradient with elevation lines (a different
 * pattern per [seed]). With [onBack] it carries the back button; with [onPickCover] (the creator) tapping it changes the photo.
 */
@Composable
fun CoverHero(
    coverUrl: String?,
    seed: Int,
    modifier: Modifier = Modifier,
    height: Dp = CoverHeroHeight,
    isBusy: Boolean = false,
    onBack: (() -> Unit)? = null,
    onPickCover: (() -> Unit)? = null
) {
    Box(modifier
        .fillMaxWidth()
        .height(height)) {
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = stringResource(R.string.team_cover_content_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            ContourBackground(
                modifier = Modifier.fillMaxSize(),
                lineColor = Color.White,
                seed = seed,
                backgroundBrush = Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                )
            )
        }
        // A scrim keeps the back button readable on any photo.
        Box(
            Modifier
                .fillMaxWidth()
                .height(96.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
        )

        if (onPickCover != null) Box(Modifier
            .fillMaxSize()
            .clickable(onClick = onPickCover))
        if (isBusy) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        }
        if (onPickCover != null) {
            Icon(
                painter = painterResource(R.drawable.ic_edit),
                contentDescription = stringResource(R.string.challenge_change_cover),
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 12.dp + SheetOverlap)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(8.dp)
            )
        }
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Icon(
                    painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.back_button),
                    tint = Color.White
                )
            }
        }
    }
}

val CoverHeroHeight = 260.dp

/** How far the sheet below the hero slides over its lower edge (the picker icon stays above it). */
val SheetOverlap = 28.dp

@Preview(name = "Without cover", showBackground = true)
@Preview(
    name = "Without cover Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
@Composable
private fun CoverHeroPreview() {
    WandrTheme { CoverHero(coverUrl = null, seed = 42, onBack = {}, onPickCover = {}) }
}

@Preview(name = "Busy", showBackground = true)
@Composable
private fun CoverHeroBusyPreview() {
    WandrTheme { CoverHero(coverUrl = null, seed = 7, height = 160.dp, isBusy = true) }
}
