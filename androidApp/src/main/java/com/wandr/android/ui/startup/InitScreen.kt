package com.wandr.android.ui.startup

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wandr.android.R
import com.wandr.android.ui.common.LabeledLoading
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.presentation.startup.StartupIntent
import com.wandr.presentation.startup.StartupState
import com.wandr.presentation.startup.StartupViewModel
import org.koin.compose.koinInject

/** Start-up: prepares local storage and the session, then reports where to go next. */
@Composable
fun InitScreen(
    onAuthenticated: () -> Unit,
    onUnauthenticated: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StartupViewModel = koinInject()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.processIntent(StartupIntent.Start) }
    LaunchedEffect(state) {
        when (state) {
            StartupState.Authenticated -> onAuthenticated()
            StartupState.Unauthenticated -> onUnauthenticated()
            else -> Unit
        }
    }
    InitScreenContent(
        state = state,
        onRetry = { viewModel.processIntent(StartupIntent.Retry) },
        modifier = modifier
    )
}

@Composable
private fun InitScreenContent(
    state: StartupState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (state is StartupState.Failed) {
            LabeledLoading(
                isLoading = true,
                title = stringResource(R.string.init_failed_title),
                modifier = Modifier.fillMaxSize(),
                message = state.message,
                action = stringResource(R.string.retry_button),
                onAction = onRetry
            )
        } else {
            LabeledLoading(
                isLoading = true,
                title = stringResource(R.string.init_loading),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview(name = "Loading", showBackground = true, widthDp = 360, heightDp = 640)
@Preview(
    name = "Loading Dark",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = 360,
    heightDp = 640
)
@Preview(name = "Loading Tablet", showBackground = true, widthDp = 840, heightDp = 1180)
@Composable
private fun InitScreenLoadingPreview() {
    WandrTheme { InitScreenContent(state = StartupState.Loading, onRetry = {}) }
}

@Preview(name = "Failed", showBackground = true, widthDp = 360, heightDp = 640)
@Preview(
    name = "Failed Dark",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = 360,
    heightDp = 640
)
@Preview(name = "Failed Tablet", showBackground = true, widthDp = 840, heightDp = 1180)
@Composable
private fun InitScreenFailedPreview() {
    WandrTheme {
        InitScreenContent(
            state = StartupState.Failed("Database could not be opened"),
            onRetry = {})
    }
}
