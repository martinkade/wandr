package com.wandr.android.ui.common

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.util.ImageProcessing
import com.wandr.presentation.imagecrop.CropState
import com.wandr.presentation.imagecrop.ImageCropper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Reusable image cropper with a fixed [aspectRatio] (width / height). Pan and pinch to position the photo
 * under the crop window; "Done" returns a JPEG scaled down to at most [outputMaxEdgePx].
 *
 * @param imageBytes the original encoded image (JPEG, PNG, HEIC, ...)
 * @param onFailure  called when the image cannot be decoded or encoded
 */
@Composable
fun ImageCropperScreen(
    imageBytes: ByteArray,
    aspectRatio: Float,
    outputMaxEdgePx: Int,
    jpegQuality: Int,
    onCropped: (ByteArray) -> Unit,
    onCancel: () -> Unit,
    onFailure: (Throwable) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }

    val loaded by produceState<LoadedImage?>(initialValue = null, imageBytes) {
        value = runCatching {
            withContext(Dispatchers.Default) {
                LoadedImage(
                    display = ImageProcessing.decodeForDisplay(imageBytes).asImageBitmap(),
                    original = ImageProcessing.orientedSize(imageBytes)
                )
            }
        }.onFailure(onFailure).getOrNull()
    }

    val image = loaded
    if (image == null) {
        Box(modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    // The cropper works in original pixels so the output size is correct even though a downsampled bitmap is shown.
    val cropper = remember(image, aspectRatio) {
        ImageCropper(image.original.width, image.original.height, aspectRatio)
    }
    var state by remember(cropper) { mutableStateOf(cropper.state) }

    ImageCropperLayout(
        bitmap = image.display,
        originalWidth = image.original.width,
        state = state,
        isProcessing = isProcessing,
        onLayout = { w, h -> state = cropper.layout(w, h) },
        onTransform = { pan, zoom, centroid -> state = cropper.transform(pan.x, pan.y, zoom, centroid.x, centroid.y) },
        onReset = { state = cropper.reset() },
        onCancel = onCancel,
        onDone = {
            isProcessing = true
            val rect = cropper.cropRect()
            val output = cropper.outputSize(outputMaxEdgePx)
            scope.launch {
                runCatching {
                    withContext(Dispatchers.Default) { ImageProcessing.cropAndScale(imageBytes, rect, output, jpegQuality) }
                }.onSuccess(onCropped).onFailure {
                    isProcessing = false
                    onFailure(it)
                }
            }
        },
        modifier = modifier
    )
}

private class LoadedImage(val display: ImageBitmap, val original: ImageProcessing.Size)

/** Stateless cropper UI (image + overlay + gestures + top bar); see [ImageCropperScreen]. */
@Composable
fun ImageCropperLayout(
    bitmap: ImageBitmap,
    originalWidth: Int,
    state: CropState,
    isProcessing: Boolean,
    onLayout: (width: Float, height: Float) -> Unit,
    onTransform: (pan: Offset, zoom: Float, centroid: Offset) -> Unit,
    onReset: () -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize().background(Color.Black)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { onLayout(it.width.toFloat(), it.height.toFloat()) }
                .pointerInput(isProcessing) {
                    if (!isProcessing) detectTransformGestures { centroid, pan, zoom, _ -> onTransform(pan, zoom, centroid) }
                }
        ) {
            if (!state.isLaidOut) return@Canvas
            // state.scale is container px per ORIGINAL pixel; the bitmap may be a downsampled version.
            val displayScale = state.scale * originalWidth / bitmap.width
            val w = bitmap.width * displayScale
            val h = bitmap.height * displayScale
            drawImage(
                image = bitmap,
                dstOffset = IntOffset((state.imageCenterX - w / 2f).toInt(), (state.imageCenterY - h / 2f).toInt()),
                dstSize = IntSize(w.toInt() + 1, h.toInt() + 1),
                filterQuality = FilterQuality.High
            )
        }
        CropOverlay(state)

        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel, enabled = !isProcessing) {
                    Text(stringResource(R.string.cancel_button), color = Color.White)
                }
                Text(stringResource(R.string.crop_title), color = Color.White, style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onDone, enabled = !isProcessing) {
                    if (isProcessing) CircularProgressIndicator(Modifier.padding(4.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text(stringResource(R.string.crop_done), color = Color.White)
                }
            }
            Box(Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.crop_hint), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onReset, enabled = !isProcessing) {
                    Text(stringResource(R.string.crop_reset), color = Color.White)
                }
            }
        }
    }
}

private fun previewBitmap(): ImageBitmap {
    val bmp = android.graphics.Bitmap.createBitmap(400, 300, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    canvas.drawColor(android.graphics.Color.rgb(46, 125, 50))
    val paint = android.graphics.Paint().apply { color = android.graphics.Color.WHITE }
    canvas.drawCircle(200f, 150f, 80f, paint)
    return bmp.asImageBitmap()
}

@Composable
private fun CropperPreviewContent(aspectRatio: Float) {
    MaterialTheme {
        val bitmap = remember { previewBitmap() }
        val cropper = remember(aspectRatio) { ImageCropper(400, 300, aspectRatio) }
        var state by remember { mutableStateOf(cropper.state) }
        ImageCropperLayout(
            bitmap = bitmap, originalWidth = 400, state = state, isProcessing = false,
            onLayout = { w, h -> state = cropper.layout(w, h) },
            onTransform = { pan, zoom, c -> state = cropper.transform(pan.x, pan.y, zoom, c.x, c.y) },
            onReset = { state = cropper.reset() }, onCancel = {}, onDone = {}
        )
    }
}

@Preview(name = "Square", widthDp = 360, heightDp = 640)
@Preview(name = "Square Dark", widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Square Tablet", widthDp = 840, heightDp = 1180)
@Composable
private fun CropperSquarePreview() = CropperPreviewContent(aspectRatio = 1f)

@Preview(name = "16:9", widthDp = 360, heightDp = 640)
@Composable
private fun CropperWidePreview() = CropperPreviewContent(aspectRatio = 16f / 9f)
