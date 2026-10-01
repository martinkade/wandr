package com.wandr.android.ui.profile

import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.wandr.android.R
import com.wandr.android.ui.common.ImageCropperScreen
import com.wandr.presentation.imagecrop.AvatarImageSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Avatar with its whole edit flow: source selection (library / camera / remove) -> 1:1 crop -> [onAvatarReady]
 * with a JPEG already scaled to [AvatarImageSpec.MAX_EDGE_PX].
 */
@Composable
fun AvatarEditor(
    avatarUrl: String?,
    displayName: String,
    isBusy: Boolean,
    onAvatarReady: (ByteArray) -> Unit,
    onRemoveAvatar: () -> Unit,
    modifier: Modifier = Modifier
) {
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

    val hasCamera = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    AvatarPicker(
        avatarUrl = avatarUrl,
        displayName = displayName,
        onPickAvatar = { showSources = true },
        isBusy = isBusy,
        modifier = modifier
    )

    if (showSources) {
        AvatarSourceDialog(
            canTakePhoto = hasCamera,
            canRemove = avatarUrl != null,
            onChooseFromLibrary = {
                showSources = false
                pickLibrary.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onTakePhoto = {
                showSources = false
                val file = File(File(context.cacheDir, "camera").apply { mkdirs() }, "avatar_capture.jpg")
                cameraFile = file
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                takePhoto.launch(uri)
            },
            onRemove = {
                showSources = false
                onRemoveAvatar()
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
                aspectRatio = AvatarImageSpec.ASPECT_RATIO,
                outputMaxEdgePx = AvatarImageSpec.MAX_EDGE_PX,
                jpegQuality = AvatarImageSpec.JPEG_QUALITY,
                onCropped = { jpeg ->
                    cropSource = null
                    onAvatarReady(jpeg)
                },
                onCancel = { cropSource = null },
                onFailure = {
                    cropSource = null
                    fail()
                }
            )
        }
    }
}
