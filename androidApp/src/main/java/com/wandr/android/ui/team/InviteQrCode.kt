package com.wandr.android.ui.team

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.wandr.android.ui.theme.WandrTheme

/**
 * A QR code of [content] (the invite code of a group; the scanner of "join a group" reads it back as the code). Always
 * black on white with a quiet zone around it, also in dark mode: scanners need that contrast.
 *
 * @param size the edge of the whole square, quiet zone included
 */
@Composable
fun InviteQrCode(content: String, modifier: Modifier = Modifier, size: Dp = 200.dp) {
    val quietZone = 12.dp
    val matrix = remember(content) {
        runCatching {
            QRCodeWriter().encode(
                content, BarcodeFormat.QR_CODE, 0, 0,
                mapOf(
                    EncodeHintType.MARGIN to 0,
                    EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
                )
            )
        }.getOrNull()
    }
    matrix ?: return
    Canvas(
        modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(quietZone)
            .size(size - quietZone * 2)
    ) {
        val module = this.size.width / matrix.width
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix[x, y]) {
                    drawRect(
                        Color.Black,
                        Offset(x * module, y * module),
                        Size(module + 0.5f, module + 0.5f)
                    )
                }
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun InviteQrCodePreview() {
    WandrTheme { InviteQrCode("X7K9P2W1", Modifier.padding(16.dp)) }
}
