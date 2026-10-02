package com.wandr.wear.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.wandr.wear.ui.WearPreviews
import com.wandr.wear.ui.theme.WandrWearTheme

/** A label with a value below it. */
@Composable
fun WorkoutStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@WearPreviews
@Composable
private fun WorkoutStatPreview() {
    WandrWearTheme { WorkoutStat(label = "Distance", value = "12.34 km") }
}
