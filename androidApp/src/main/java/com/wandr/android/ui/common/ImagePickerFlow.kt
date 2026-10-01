package com.wandr.android.ui.common

import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.wandr.android.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Call [open] to start: source selection -> crop with the configured aspect ratio -> `onImageReady`. */
@Stable
class ImagePickerFlow internal constructor(private val onOpen: () -> Unit) {
    fun open() = onOpen()
}

/**
 * Reusable image flow for any picture with a fixed [aspectRatio] (avatar 1:1, cover 4:3, ...).
 * It renders its own dialogs; [onImageReady] receives a JPEG already scaled to [outputMaxEdgePx].
 */
@Composable
fun rememberImagePickerFlow(
    title: String,
    aspectRatio: Float,
    outputMaxEdgePx: Int,
    jpegQuality: Int,
    canRemove: Boolean,
    onImageReady: (ByteArray) -> Unit,
    onRemove: () -> Unit
): ImagePickerFlow {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSources by remember { mutableStateOf(false) }
    var cropSource by remember { mutableStateOf<ByteArray?>(null) }
    var cameraFile by remember { mutableStateOf<File?>(null) }
    val loadFailed = stringResource(R.string.image_load_failed)

    fun fail() = Toast.makeText(context, loadFailed, Toast.LENGTH_LONG).show()

    val pickLibrary = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            }
            if (bytes == null || bytes.isEmpty()) fail() else cropSource = bytes
        }
    }

    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved: Boolean ->
        val file = cameraFile
        cameraFile = null
        if (!saved || file == null) {
            file?.delete()
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                runCatching { file.readBytes() }.getOrNull().also { file.delete() }
            }
            if (bytes == null || bytes.isEmpty()) fail() else cropSource = bytes
        }
    }

    // The system camera app needs no CAMERA permission, so no disclosure is required here.
    val hasCamera = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }

    if (showSources) {
        ImageSourceDialog(
            title = title,
            canTakePhoto = hasCamera,
            canRemove = canRemove,
            onChooseFromLibrary = {
                showSources = false
                pickLibrary.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onTakePhoto = {
                showSources = false
                val file = File(File(context.cacheDir, "camera").apply { mkdirs() }, "capture.jpg")
                cameraFile = file
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                takePhoto.launch(uri)
            },
            onRemove = {
                showSources = false
                onRemove()
            },
            onDismiss = { showSources = false }
        )
    }

    cropSource?.let { source ->
        Dialog(
            onDismissRequest = { cropSource = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            ImageCropperScreen(
                imageBytes = source,
                aspectRatio = aspectRatio,
                outputMaxEdgePx = outputMaxEdgePx,
                jpegQuality = jpegQuality,
                onCropped = { jpeg ->
                    cropSource = null
                    onImageReady(jpeg)
                },
                onCancel = { cropSource = null },
                onFailure = {
                    cropSource = null
                    fail()
                }
            )
        }
    }

    return remember { ImagePickerFlow { showSources = true } }
}
