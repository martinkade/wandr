package com.wandr.android.ui.common

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.theme.WandrTheme

/** The launcher logo (foreground on the launcher's background color) as a rounded square. */
@Composable
fun AppLogo(modifier: Modifier = Modifier, size: Dp = 88.dp) {
    Box(
        modifier = modifier
            .requiredSize(size)
            .clip(RoundedCornerShape(size * 0.24f))
            .background(colorResource(R.color.ic_launcher_background)),
        contentAlignment = Alignment.Center
    ) {
        // The launcher foreground is a 108dp canvas whose logo sits in the inner 72dp; enlarging by that factor
        // makes the logo fill the square.
        Image(
            painter = painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.requiredSize(size * (108f / 72f))
        )
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun AppLogoPreview() {
    WandrTheme { Box(Modifier.padding(16.dp)) { AppLogo() } }
}
