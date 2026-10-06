package com.wandr.android.ui.activity

import android.content.res.Configuration
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.common.DateTimePickerField
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.presentation.activity.ActivityForm
import com.wandr.presentation.activity.ActivityIntent
import com.wandr.presentation.activity.ActivityTypes
import java.util.Locale

/**
 * Create / edit form shown in a bottom sheet (manual entries; for recorded activities only title, description and type
 * can change, the measured values are shown but locked).
 */
@Composable
fun ActivityEditScreen(
    form: ActivityForm,
    isSaving: Boolean,
    onIntent: (ActivityIntent) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    // Local text so partial input like "12," does not get rewritten while typing; reset when another activity is shown.
    var distanceText by remember(form.activityId) { mutableStateOf(formatNumber(form.distanceKm)) }
    var durationText by remember(form.activityId) { mutableStateOf(formatNumber(form.durationMinutes)) }
    var elevationText by remember(form.activityId) { mutableStateOf(formatNumber(form.elevationMeters)) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(if (form.isEditing) R.string.activity_edit_title else R.string.activity_new_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.tertiary
        )
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = form.title,
            onValueChange = { onIntent(ActivityIntent.TitleChanged(it)) },
            label = { Text(stringResource(R.string.activity_title_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = form.description,
            onValueChange = { onIntent(ActivityIntent.DescriptionChanged(it)) },
            label = { Text(stringResource(R.string.activity_description_label)) },
            modifier = Modifier.fillMaxWidth().height(100.dp)
        )

        Text(
            text = stringResource(R.string.activity_type_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp)
        )
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActivityTypes.all.forEach { type ->
                FilterChip(
                    selected = form.activityType == type,
                    onClick = { onIntent(ActivityIntent.TypeChanged(type)) },
                    label = { Text(activityTypeText(type)) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        val measuredEditable = !form.isMeasured
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(
                value = distanceText,
                label = stringResource(R.string.distance_label),
                enabled = measuredEditable,
                modifier = Modifier.weight(1f)
            ) { text, number ->
                distanceText = text
                number?.let { onIntent(ActivityIntent.DistanceChanged(it)) }
            }
            NumberField(
                value = durationText,
                label = stringResource(R.string.duration_label),
                enabled = measuredEditable,
                modifier = Modifier.weight(1f)
            ) { text, number ->
                durationText = text
                number?.let { onIntent(ActivityIntent.DurationChanged(it)) }
            }
        }
        Spacer(Modifier.height(12.dp))
        NumberField(
            value = elevationText,
            label = stringResource(R.string.elevation_label),
            enabled = measuredEditable,
            modifier = Modifier.fillMaxWidth()
        ) { text, number ->
            elevationText = text
            number?.let { onIntent(ActivityIntent.ElevationChanged(it)) }
        }
        Spacer(Modifier.height(12.dp))
        if (measuredEditable) {
            DateTimePickerField(
                label = stringResource(R.string.challenge_start_label),
                valueMillis = form.startTime,
                onValueChange = { onIntent(ActivityIntent.StartTimeChanged(it)) }
            )
        } else {
            Text(
                text = stringResource(R.string.activity_measured_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (form.hasRoute) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(stringResource(R.string.activity_show_map_label), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = stringResource(R.string.activity_show_map_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = form.showMap, onCheckedChange = { onIntent(ActivityIntent.ShowMapChanged(it)) })
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, enabled = !isSaving, modifier = Modifier.weight(1f).height(50.dp)) {
                Text(stringResource(R.string.cancel_button))
            }
            Button(onClick = onSave, enabled = !isSaving, modifier = Modifier.weight(1f).height(50.dp)) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.save_activity_button))
                }
            }
        }

        // Messages while the sheet is open (e.g. validation or server errors) are shown here, above the sheet.
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(vertical = 16.dp))
    }
}

@Composable
private fun NumberField(
    value: String,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onChange: (text: String, number: Double?) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onChange(text, text.replace(',', '.').toDoubleOrNull()) },
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}

/** "5" instead of "5.0", up to two decimals otherwise. */
private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')

@Preview(name = "Create", showBackground = true, heightDp = 900)
@Preview(name = "Create Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, heightDp = 900)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true, heightDp = 1200)
@Composable
private fun ActivityEditScreenCreatePreview() {
    WandrTheme {
        ActivityEditScreen(
            form = ActivityForm(title = "Morning hike", startTime = 1_768_435_200_000L),
            isSaving = false, onIntent = {}, onSave = {}, onCancel = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(name = "Edit recorded (locked values)", showBackground = true, heightDp = 900)
@Preview(name = "Tablet", widthDp = 840, heightDp = 900, showBackground = true)
@Composable
private fun ActivityEditScreenMeasuredPreview() {
    WandrTheme {
        ActivityEditScreen(
            form = ActivityForm(
                activityId = "a1", title = "Evening run", activityType = "running", distanceKm = 10.0,
                durationMinutes = 50.0, elevationMeters = 85.0, startTime = 1_768_435_200_000L, isMeasured = true
            ),
            isSaving = false, onIntent = {}, onSave = {}, onCancel = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
