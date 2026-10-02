package com.wandr.wear.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.wandr.wear.R
import com.wandr.wear.model.WearActivityType
import com.wandr.wear.ui.WearPreviews
import com.wandr.wear.ui.theme.WandrWearTheme

@Composable
fun StartScreen(
    type: WearActivityType,
    busy: Boolean,
    startFailed: Boolean,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(type.labelRes), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            text = stringResource(if (startFailed) R.string.start_failed else R.string.start_hint),
            style = MaterialTheme.typography.bodySmall,
            color = if (startFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onStart,
            enabled = !busy,
            label = { Text(stringResource(if (busy) R.string.starting else R.string.start_button)) }
        )
    }
}

@WearPreviews
@Composable
private fun StartScreenPreview() {
    WandrWearTheme { StartScreen(type = WearActivityType.HIKING, busy = false, startFailed = false, onStart = {}) }
}
