package com.wandr.android.ui.startup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.android.R
import com.wandr.presentation.startup.StartupState

@Composable
fun InitScreen(
    state: StartupState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state is StartupState.Failed) {
                Text(
                    text = stringResource(R.string.init_failed_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Button(onClick = onRetry) { Text(stringResource(R.string.retry_button)) }
            } else {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.init_loading),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Preview(name = "Loading", showBackground = true, widthDp = 360, heightDp = 640)
@Preview(name = "Loading Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360, heightDp = 640)
@Preview(name = "Loading Tablet", showBackground = true, widthDp = 840, heightDp = 1180)
@Composable
private fun InitScreenLoadingPreview() {
    WandrTheme { InitScreen(state = StartupState.Loading, onRetry = {}) }
}

@Preview(name = "Failed", showBackground = true, widthDp = 360, heightDp = 640)
@Preview(name = "Failed Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360, heightDp = 640)
@Preview(name = "Failed Tablet", showBackground = true, widthDp = 840, heightDp = 1180)
@Composable
private fun InitScreenFailedPreview() {
    WandrTheme { InitScreen(state = StartupState.Failed("Database could not be opened"), onRetry = {}) }
}
