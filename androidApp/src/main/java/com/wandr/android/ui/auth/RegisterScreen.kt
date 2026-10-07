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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
fun RegisterScreen(
    onAuthenticated: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = koinInject()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state.isAuthenticated) { if (state.isAuthenticated) onAuthenticated() }
    RegisterScreenContent(
        state = state,
        onIntent = viewModel::processIntent,
        onNavigateToLogin = onNavigateToLogin,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterScreenContent(
    state: LoginState,
    onIntent: (LoginIntent) -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    ContourBackground(modifier = modifier.fillMaxSize()) {
        // The card is centered; when it is taller than the free space (soft keyboard, small screen, large font) the page scrolls.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
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
                    text = stringResource(R.string.join_wandr_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = stringResource(R.string.join_wandr_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = state.emailInput,
                    onValueChange = { onIntent(LoginIntent.EmailChanged(it)) },
                    label = { Text(stringResource(R.string.email_address_label)) },
                    isError = state.error.concernsEmail(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = state.passwordInput,
                    onValueChange = { onIntent(LoginIntent.PasswordChanged(it)) },
                    label = { Text(stringResource(R.string.password_hint)) },
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
                        onClick = { onIntent(LoginIntent.SubmitRegister) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(stringResource(R.string.create_account_button))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onNavigateToLogin) {
                    Text(stringResource(R.string.already_have_account))
                }
            }
        }
    }

    // The email address has to be confirmed first: tell the user and lead them to the sign-in screen.
    if (state.needsEmailConfirmation) {
        val continueToLogin = {
            onIntent(LoginIntent.ContinueToLogin)
            onNavigateToLogin()
        }
        ModalBottomSheet(
            onDismissRequest = continueToLogin,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.registration_success_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.registration_success_confirm_email),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = continueToLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(stringResource(R.string.registration_sign_in_now))
                }
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Large Font 1.5x", fontScale = 1.5f, showBackground = true)
@Composable
private fun RegisterScreenPreview() {
    WandrTheme {
        RegisterScreenContent(
            state = LoginState(emailInput = "newuser@example.com", passwordInput = "secret123"),
            onIntent = {},
            onNavigateToLogin = {}
        )
    }
}

@Preview(name = "Account created", showBackground = true)
@Preview(
    name = "Account created, confirm email Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
@Composable
private fun RegisterScreenSuccessPreview() {
    WandrTheme {
        RegisterScreenContent(
            state = LoginState(
                emailInput = "newuser@example.com",
                needsEmailConfirmation = true
            ),
            onIntent = {},
            onNavigateToLogin = {}
        )
    }
}
