package com.wandr.android.ui.activity

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.android.util.AppDateFormatter
import com.wandr.domain.model.Activity
import com.wandr.domain.model.ConflictResolution
import com.wandr.presentation.activity.ActivityConflict

/**
 * Conflict resolution wizard: lists the overlapping activities and lets the user merge, trim or discard. The choice
 * is only applied after confirming, because merging replaces the existing activities.
 *
 * @param onDismiss null when the conflict cannot be dismissed (a finished recording would be lost)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityConflictSheet(
    conflict: ActivityConflict,
    isSaving: Boolean,
    onResolve: (ConflictResolution) -> Unit,
    onDismiss: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss?.invoke() },
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { !isSaving && onDismiss != null }
        ),
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) {
        ActivityConflictContent(conflict, isSaving, onResolve, onDismiss)
    }
}

@Composable
private fun ActivityConflictContent(
    conflict: ActivityConflict,
    isSaving: Boolean,
    onResolve: (ConflictResolution) -> Unit,
    onDismiss: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    var selected by rememberSaveable { mutableStateOf<ConflictResolution?>(null) }
    val locale = LocalConfiguration.current.locales[0]

    Column(modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.activity_conflict_title), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.activity_conflict_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        conflict.conflicting.forEach { activity ->
            Column {
                Text(activity.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "${AppDateFormatter.formatDateTime(activity.startTime, locale = locale)} – " +
                        AppDateFormatter.formatDateTime(activity.endTime, locale = locale),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(Modifier.selectableGroup()) {
            ResolutionOption(
                selected = selected == ConflictResolution.MERGE,
                title = R.string.activity_conflict_merge_title,
                description = R.string.activity_conflict_merge_description,
                enabled = !isSaving,
                onSelect = { selected = ConflictResolution.MERGE }
            )
            ResolutionOption(
                selected = selected == ConflictResolution.TRIM,
                title = R.string.activity_conflict_trim_title,
                description = if (conflict.canTrim) R.string.activity_conflict_trim_description
                else R.string.activity_conflict_trim_unavailable,
                enabled = !isSaving && conflict.canTrim,
                onSelect = { selected = ConflictResolution.TRIM }
            )
            ResolutionOption(
                selected = selected == ConflictResolution.DISCARD,
                title = R.string.activity_conflict_discard_title,
                description = R.string.activity_conflict_discard_description,
                enabled = !isSaving,
                onSelect = { selected = ConflictResolution.DISCARD }
            )
        }

        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (onDismiss != null) {
                OutlinedButton(onClick = onDismiss, enabled = !isSaving, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
            Button(
                onClick = { selected?.let(onResolve) },
                enabled = selected != null && !isSaving,
                modifier = Modifier.weight(1f)
            ) {
                if (isSaving) CircularProgressIndicator(Modifier.padding(2.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.activity_conflict_apply_button))
            }
        }
    }
}

@Composable
private fun ResolutionOption(
    selected: Boolean,
    title: Int,
    description: Int,
    enabled: Boolean,
    onSelect: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Column(Modifier.padding(start = 12.dp)) {
            Text(
                stringResource(title),
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                stringResource(description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val previewConflict = ActivityConflict(
    conflicting = listOf(
        Activity(
            id = "a1", userId = "u1", teamId = null, title = "Evening run", description = null, activityType = "running",
            distanceMeters = 10_000.0, durationSeconds = 3600.0, elevationGainMeters = 50.0, fitFilePath = null,
            startTime = 1_768_435_200_000L, endTime = 1_768_438_800_000L, isManualEntry = true, createdAt = 0L, updatedAt = 0L
        )
    ),
    canTrim = true,
    dismissible = true
)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true, heightDp = 1100)
@Composable
private fun ActivityConflictContentPreview() {
    WandrTheme { ActivityConflictContent(previewConflict, isSaving = false, onResolve = {}, onDismiss = {}) }
}

@Preview(name = "Not trimmable, recording", showBackground = true)
@Preview(name = "Saving Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun ActivityConflictContentRecordingPreview() {
    WandrTheme {
        ActivityConflictContent(
            previewConflict.copy(canTrim = false, dismissible = false), isSaving = false, onResolve = {}, onDismiss = null
        )
    }
}
