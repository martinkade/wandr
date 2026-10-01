package com.wandr.android.ui.common

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.presentation.imagecrop.CropState

/** Dims everything outside the crop window and draws its border and thirds grid. */
@Composable
fun CropOverlay(state: CropState, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (!state.isLaidOut) return@Canvas
        val dim = Color.Black.copy(alpha = 0.6f)
        val l = state.windowLeft
        val t = state.windowTop
        val r = l + state.windowWidth
        val b = t + state.windowHeight

        drawRect(dim, Offset.Zero, Size(size.width, t))
        drawRect(dim, Offset(0f, b), Size(size.width, size.height - b))
        drawRect(dim, Offset(0f, t), Size(l, b - t))
        drawRect(dim, Offset(r, t), Size(size.width - r, b - t))

        val grid = Color.White.copy(alpha = 0.35f)
        val hairline = 1.dp.toPx()
        for (i in 1..2) {
            val x = l + state.windowWidth * i / 3f
            val y = t + state.windowHeight * i / 3f
            drawLine(grid, Offset(x, t), Offset(x, b), hairline)
            drawLine(grid, Offset(l, y), Offset(r, y), hairline)
        }
        drawRect(Color.White, Offset(l, t), Size(r - l, b - t), style = Stroke(width = 2.dp.toPx()))
    }
}

@Preview(name = "Square", widthDp = 360, heightDp = 640)
@Preview(name = "Square Dark", widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Wide Tablet", widthDp = 840, heightDp = 600)
@Composable
private fun CropOverlayPreview() {
    WandrTheme {
        val cropper = com.wandr.presentation.imagecrop.ImageCropper(2000, 1500, aspectRatio = 1f)
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize().background(Color.DarkGray)) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val state = with(density) { cropper.layout(maxWidth.toPx(), maxHeight.toPx()) }
            CropOverlay(state)
        }
    }
}
