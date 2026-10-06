package com.wandr.android.ui.challenge

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import com.wandr.android.ui.activity.activityTypeText
import com.wandr.android.ui.common.DateTimePickerField
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.model.ChallengeScope
import com.wandr.domain.model.ChallengeType
import com.wandr.presentation.challenge.ChallengeForm
import com.wandr.presentation.activity.ActivityTypes
import com.wandr.presentation.challenge.ChallengeIntent
import com.wandr.presentation.challenge.ChallengeUnits
import java.util.Locale

/**
 * Create / edit form shown in the challenge bottom sheet. Targets are entered in friendly units
 * (km, m, hours) and converted to base units by [ChallengeUnits].
 */
@Composable
fun ChallengeEditScreen(
    form: ChallengeForm,
    isSaving: Boolean,
    onIntent: (ChallengeIntent) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    // Local text so partial input like "12," does not get rewritten while typing; reset on type / challenge change.
    var targetText by remember(form.challengeId, form.type) {
        mutableStateOf(formatDisplay(ChallengeUnits.toDisplay(form.type, form.targetValue)))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(if (form.isEditing) R.string.challenge_edit_title else R.string.create_challenge_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.tertiary
        )
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = form.title,
            onValueChange = { onIntent(ChallengeIntent.TitleChanged(it)) },
            label = { Text(stringResource(R.string.challenge_title_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = form.description,
            onValueChange = { onIntent(ChallengeIntent.DescriptionChanged(it)) },
            label = { Text(stringResource(R.string.challenge_description_label)) },
            modifier = Modifier.fillMaxWidth().height(100.dp)
        )

        SectionLabel(R.string.challenge_type_label)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChallengeType.entries.forEach { type ->
                FilterChip(
                    selected = form.type == type,
                    onClick = { onIntent(ChallengeIntent.TypeChanged(type)) },
                    label = { Text(stringResource(typeLabel(type))) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = targetText,
            onValueChange = { text ->
                targetText = text
                text.replace(',', '.').toDoubleOrNull()?.let {
                    onIntent(ChallengeIntent.TargetValueChanged(ChallengeUnits.toBase(form.type, it)))
                }
            },
            label = { Text(stringResource(R.string.challenge_target_label)) },
            suffix = { Text(stringResource(unitLabel(form.type))) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        // Challenges can be planned for the future: pick start and end date + time.
        Spacer(Modifier.height(12.dp))
        DateTimePickerField(
            label = stringResource(R.string.challenge_start_label),
            valueMillis = form.startDate,
            onValueChange = { onIntent(ChallengeIntent.StartDateChanged(it)) }
        )
        Spacer(Modifier.height(8.dp))
        DateTimePickerField(
            label = stringResource(R.string.challenge_end_label),
            valueMillis = form.endDate,
            onValueChange = { onIntent(ChallengeIntent.EndDateChanged(it)) }
        )

        SectionLabel(R.string.challenge_activity_types_label)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActivityTypes.all.forEach { type ->
                FilterChip(
                    selected = type in form.activityTypes,
                    onClick = { onIntent(ChallengeIntent.ActivityTypeToggled(type)) },
                    label = { Text(activityTypeText(type)) }
                )
            }
        }
        Text(
            text = stringResource(R.string.challenge_activity_types_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )

        SectionLabel(R.string.challenge_scope_label)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = form.scope == ChallengeScope.INDIVIDUAL,
                onClick = { onIntent(ChallengeIntent.ScopeChanged(ChallengeScope.INDIVIDUAL)) },
                label = { Text(stringResource(R.string.challenge_scope_individual)) }
            )
            FilterChip(
                selected = form.scope == ChallengeScope.GROUP,
                onClick = { onIntent(ChallengeIntent.ScopeChanged(ChallengeScope.GROUP)) },
                label = { Text(stringResource(R.string.challenge_scope_group)) }
            )
        }

        if (form.scope == ChallengeScope.GROUP) {
            Text(
                text = stringResource(R.string.challenge_scope_group_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.require_all_members_label),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                )
                Switch(
                    checked = form.requireAllMembersCompletion,
                    onCheckedChange = { onIntent(ChallengeIntent.RequireAllMembersCompletionChanged(it)) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(stringResource(R.string.challenge_active_label), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = stringResource(R.string.challenge_active_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = form.isActive, onCheckedChange = { onIntent(ChallengeIntent.ActiveChanged(it)) })
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
                    Text(stringResource(R.string.save_challenge_button))
                }
            }
        }

        // Messages while the sheet is open (e.g. validation or server errors) are shown here, above the sheet.
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(vertical = 16.dp))
    }
}

@Composable
private fun SectionLabel(textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp)
    )
}

private fun typeLabel(type: ChallengeType) = when (type) {
    ChallengeType.DISTANCE -> R.string.challenge_type_distance
    ChallengeType.ELEVATION -> R.string.challenge_type_elevation
    ChallengeType.TIME -> R.string.challenge_type_time
}

private fun unitLabel(type: ChallengeType) = when (type) {
    ChallengeType.DISTANCE -> R.string.challenge_unit_distance
    ChallengeType.ELEVATION -> R.string.challenge_unit_elevation
    ChallengeType.TIME -> R.string.challenge_unit_time
}

/** "100" instead of "100.0", up to two decimals otherwise. */
private fun formatDisplay(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')

@Preview(name = "Create", showBackground = true)
@Preview(name = "Create Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Composable
private fun ChallengeEditScreenPreview() {
    WandrTheme {
        ChallengeEditScreen(
            form = ChallengeForm(title = "30-Day 100 km Hike", startDate = 1_768_435_200_000L, endDate = 1_771_027_200_000L),
            isSaving = false,
            onIntent = {}, onSave = {}, onCancel = {}, snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(name = "Edit group challenge", showBackground = true, heightDp = 900)
@Preview(name = "Edit group Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, heightDp = 900)
@Preview(name = "Tablet", widthDp = 840, heightDp = 900, showBackground = true)
@Composable
private fun ChallengeEditScreenGroupPreview() {
    WandrTheme {
        ChallengeEditScreen(
            form = ChallengeForm(
                challengeId = "c1", title = "Team sprint", description = "Everybody finishes.",
                type = ChallengeType.ELEVATION, targetValue = 5000.0, scope = ChallengeScope.GROUP,
                startDate = 1_768_435_200_000L, endDate = 1_771_027_200_000L,
                requireAllMembersCompletion = true, isActive = true
            ),
            isSaving = false,
            onIntent = {}, onSave = {}, onCancel = {}, snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
