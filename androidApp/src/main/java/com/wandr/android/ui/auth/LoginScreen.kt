package com.wandr.android.ui.auth

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.common.AppLogo
import com.wandr.android.ui.common.ContourBackground
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.presentation.auth.LoginIntent
import com.wandr.presentation.auth.LoginState
import com.wandr.presentation.auth.LoginViewModel
import org.koin.compose.koinInject

@Composable
fun LoginScreen(
    onAuthenticated: () -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = koinInject()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state.isAuthenticated) { if (state.isAuthenticated) onAuthenticated() }
    LoginScreenContent(
        state = state,
        onIntent = viewModel::processIntent,
        onNavigateToRegister = onNavigateToRegister,
        modifier = modifier
    )
}

@Composable
private fun LoginScreenContent(
    state: LoginState,
    onIntent: (LoginIntent) -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    ContourBackground(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .background(
                    color = CardDefaults.cardColors().containerColor,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(top = 32.dp, start = 16.dp, end = 16.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppLogo()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.app_name))
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Light
                        )
                    ) {
                        append(" | ")
                    }
                    withStyle(
                        SpanStyle(
                            fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(stringResource(R.string.login_title))
                    }
                },
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = state.emailInput,
                onValueChange = { onIntent(LoginIntent.EmailChanged(it)) },
                label = { Text(stringResource(R.string.email_label)) },
                isError = state.error.concernsEmail(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.passwordInput,
                onValueChange = { onIntent(LoginIntent.PasswordChanged(it)) },
                label = { Text(stringResource(R.string.password_label)) },
                isError = state.error.concernsPassword(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            state.error?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                AuthErrorText(error)
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { onIntent(LoginIntent.SubmitLogin) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(stringResource(R.string.sign_in_button))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onNavigateToRegister) {
                Text(stringResource(R.string.dont_have_account))
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Large Font 1.5x", fontScale = 1.5f, showBackground = true)
@Composable
private fun LoginScreenPreview() {
    WandrTheme {
        LoginScreenContent(
            state = LoginState(emailInput = "user@example.com", passwordInput = "secret"),
            onIntent = {},
            onNavigateToRegister = {}
        )
    }
}
