package com.wandr.android.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.theme.WandrTheme

/** Shown by Health Connect ("privacy policy" link of the permission screen): why the app reads workouts. */
class HealthPermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WandrTheme { HealthPermissionsRationale(onClose = ::finish) } }
    }
}

@Composable
private fun HealthPermissionsRationale(onClose: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.health_connect_disclosure_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.health_connect_disclosure_message), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onClose) { Text(stringResource(R.string.health_connect_rationale_close)) }
        }
    }
}
