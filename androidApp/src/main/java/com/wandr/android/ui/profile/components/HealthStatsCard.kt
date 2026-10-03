package com.wandr.android.ui.profile.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.wandr.android.R
import com.wandr.android.health.DailyHealthSummary
import com.wandr.android.health.HealthConnectImporter
import com.wandr.android.ui.theme.WandrTheme
import java.text.NumberFormat

/**
 * Today's steps and floors climbed from Health Connect. Shows nothing unless the connection is enabled (Health Connect
 * available and at least steps or floors permitted); refreshes whenever the app is resumed.
 */
@Composable
fun HealthStatsCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val importer = remember { HealthConnectImporter(context) }
    var summary by remember { mutableStateOf<DailyHealthSummary?>(null) }
    var refresh by remember { mutableIntStateOf(0) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    LaunchedEffect(refresh) {
        summary = if (importer.isAvailable) runCatching { importer.readToday() }.getOrNull() else null
    }

    summary?.let { HealthStatsCardContent(it, modifier) }
}

@Composable
private fun HealthStatsCardContent(summary: DailyHealthSummary, modifier: Modifier = Modifier) {
    val format = NumberFormat.getIntegerInstance(LocalConfiguration.current.locales[0])
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.health_stats_title), style = MaterialTheme.typography.titleMedium)
            summary.steps?.let { Stat(format.format(it), stringResource(R.string.health_stats_steps)) }
            summary.floors?.let { Stat(format.format(it.toLong()), stringResource(R.string.health_stats_floors)) }
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.tertiary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Preview(name = "Tablet", widthDp = 840, heightDp = 400, showBackground = true)
@Composable
private fun HealthStatsCardPreview() {
    WandrTheme {
        HealthStatsCardContent(
            DailyHealthSummary(steps = 8_432, floors = 12.0),
            Modifier.height(240.dp).aspectRatio(1.25f)
        )
    }
}

@Preview(name = "Steps only", showBackground = true)
@Composable
private fun HealthStatsCardStepsOnlyPreview() {
    WandrTheme {
        HealthStatsCardContent(DailyHealthSummary(steps = 1_204, floors = null), Modifier.height(240.dp).aspectRatio(1.25f))
    }
}
