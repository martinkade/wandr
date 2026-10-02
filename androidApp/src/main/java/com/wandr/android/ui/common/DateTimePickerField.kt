package com.wandr.android.ui.common

import android.content.res.Configuration
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.android.util.AppDateFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

private enum class PickerStep { None, Date, Time }

/**
 * Field showing a date and time; tapping it opens a date picker followed by a time picker (local time zone).
 *
 * @param valueMillis epoch milliseconds
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerField(
    label: String,
    valueMillis: Long,
    onValueChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableStateOf(PickerStep.None) }
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }
    val locale = LocalConfiguration.current.locales[0]
    val zone = ZoneId.systemDefault()
    val current = remember(valueMillis, zone) { Instant.ofEpochMilli(valueMillis).atZone(zone).toLocalDateTime() }

    OutlinedCard(onClick = { step = PickerStep.Date }, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = AppDateFormatter.formatDateTime(valueMillis, locale = locale, zone = zone),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }

    when (step) {
        PickerStep.Date -> {
            // The date picker works with UTC midnight of the chosen day.
            val dateState = rememberDatePickerState(
                initialSelectedDateMillis = current.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            )
            DatePickerDialog(
                onDismissRequest = { step = PickerStep.None },
                confirmButton = {
                    TextButton(
                        enabled = dateState.selectedDateMillis != null,
                        onClick = {
                            pickedDate = dateState.selectedDateMillis?.let {
                                Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                            }
                            step = PickerStep.Time
                        }
                    ) { Text(stringResource(R.string.ok_button)) }
                },
                dismissButton = { TextButton(onClick = { step = PickerStep.None }) { Text(stringResource(R.string.cancel_button)) } }
            ) { DatePicker(state = dateState) }
        }
        PickerStep.Time -> {
            val timeState = rememberTimePickerState(
                initialHour = current.hour,
                initialMinute = current.minute,
                is24Hour = DateFormat.is24HourFormat(LocalContext.current)
            )
            AlertDialog(
                onDismissRequest = { step = PickerStep.None },
                title = { Text(stringResource(R.string.picker_select_time)) },
                text = { TimePicker(state = timeState) },
                confirmButton = {
                    TextButton(onClick = {
                        val date = pickedDate ?: current.toLocalDate()
                        val picked = LocalDateTime.of(date, LocalTime.of(timeState.hour, timeState.minute))
                        onValueChange(picked.atZone(zone).toInstant().toEpochMilli())
                        step = PickerStep.None
                    }) { Text(stringResource(R.string.ok_button)) }
                },
                dismissButton = { TextButton(onClick = { step = PickerStep.None }) { Text(stringResource(R.string.cancel_button)) } }
            )
        }
        PickerStep.None -> Unit
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Composable
private fun DateTimePickerFieldPreview() {
    WandrTheme {
        DateTimePickerField(
            label = "Start", valueMillis = 1_768_435_200_000L, onValueChange = {}, modifier = Modifier.padding(16.dp)
        )
    }
}
