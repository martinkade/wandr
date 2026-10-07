package com.wandr.android.ui.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Makes the screen a bit brighter while this is in the composition (e.g. a QR code that another device scans) and puts the
 * brightness back as soon as it leaves. Only the app's window is touched, the system setting stays as it is.
 *
 * @param boost how much brighter than the system setting, from 0 to 1
 */
@Composable
fun BoostScreenBrightness(boost: Float = 0.3f) {
    val context = LocalContext.current
    DisposableEffect(boost) {
        val window = context.findActivity()?.window
        if (window == null) {
            onDispose { }
        } else {
            val previous = window.attributes.screenBrightness
            // BRIGHTNESS_OVERRIDE_NONE means "as the system setting"; read that to know what to raise.
            val base = if (previous >= 0f) previous else systemBrightness(context)
            window.attributes =
                window.attributes.apply { screenBrightness = (base + boost).coerceAtMost(1f) }
            onDispose {
                window.attributes = window.attributes.apply { screenBrightness = previous }
            }
        }
    }
}

private fun systemBrightness(context: Context): Float {
    val value = runCatching {
        Settings.System.getInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS
        )
    }.getOrDefault(128)
    return (value / 255f).coerceIn(0f, 1f)
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

