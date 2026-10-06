package com.wandr.android.ui.activity.recording

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.activity.activityTypeText
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.presentation.activity.ActivityTypes

@DrawableRes
internal fun activityTypeIcon(type: String): Int = when (type) {
    "cycling" -> R.drawable.ic_directions_bike
    "hiking" -> R.drawable.ic_directions_walk
    else -> R.drawable.ic_directions_run
}

/**
 * The bottom controls: sport selector (only before the start), the big start / pause / resume button and "Finish" while
 * a recording is running.
 */
@Composable
fun RecordingControls(
    activityType: String,
    isTracking: Boolean,
    isPaused: Boolean,
    onTypeChange: (String) -> Unit,
    onPrimaryAction: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).navigationBarsPadding().padding(vertical = 16.dp, horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
            TypeSelector(activityType, enabled = !isTracking, onTypeChange)
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
            val icon = when {
                !isTracking -> R.drawable.ic_play_arrow
                isPaused -> R.drawable.ic_play_arrow
                else -> R.drawable.ic_pause
            }
            val description = stringResource(
                when {
                    !isTracking -> R.string.start_tracking_button
                    isPaused -> R.string.resume_button
                    else -> R.string.pause_button
                }
            )
            Box(
                Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).clickable(onClick = onPrimaryAction),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(icon), contentDescription = description, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(40.dp))
            }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
            if (isTracking) {
                RoundButton(R.drawable.ic_stop, stringResource(R.string.recording_finish_button), onFinish)
            }
        }
    }
}

@Composable
private fun TypeSelector(activityType: String, enabled: Boolean, onTypeChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Box(Modifier.alpha(if (enabled) 1f else 0.5f)) {
            RoundButton(activityTypeIcon(activityType), activityTypeText(activityType), onClick = { if (enabled) open = true })
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            ActivityTypes.all.forEach { type ->
                DropdownMenuItem(
                    text = { Text(activityTypeText(type)) },
                    leadingIcon = { Icon(painterResource(activityTypeIcon(type)), contentDescription = null) },
                    onClick = {
                        open = false
                        onTypeChange(type)
                    }
                )
            }
        }
    }
}

/** A round button with its label below. */
@Composable
private fun RoundButton(@DrawableRes icon: Int, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(4.dp)) {
        Box(Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(28.dp))
        }
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
    }
}

@Preview(name = "Before start", showBackground = true)
@Preview(name = "Before start Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Composable
private fun RecordingControlsIdlePreview() {
    WandrTheme { RecordingControls("running", isTracking = false, isPaused = false, onTypeChange = {}, onPrimaryAction = {}, onFinish = {}) }
}

@Preview(name = "Recording", showBackground = true)
@Preview(name = "Paused", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun RecordingControlsRunningPreview() {
    WandrTheme {
        Column {
            RecordingControls("cycling", isTracking = true, isPaused = false, onTypeChange = {}, onPrimaryAction = {}, onFinish = {})
            RecordingControls("hiking", isTracking = true, isPaused = true, onTypeChange = {}, onPrimaryAction = {}, onFinish = {})
        }
    }
}
