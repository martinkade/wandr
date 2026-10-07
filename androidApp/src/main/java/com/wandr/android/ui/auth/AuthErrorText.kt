package com.wandr.android.ui.auth

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.ui.common.userMessage
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.error.AppError

/** The error under the sign-in / sign-up form. "Confirm your email" is good news, not an error, and is shown calmly. */
@Composable
fun AuthErrorText(error: AppError, modifier: Modifier = Modifier) {
    val isInfo = error is AppError.EmailConfirmationRequired
    Text(
        text = error.userMessage(),
        style = MaterialTheme.typography.bodySmall,
        color = if (isInfo) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
        modifier = modifier
    )
}

/** The email field is marked for problems with the address. */
internal fun AppError?.concernsEmail(): Boolean =
    this is AppError.InvalidEmail || this is AppError.UserAlreadyExists || this is AppError.EmailNotConfirmed

/** The password field is marked for problems with the password. */
internal fun AppError?.concernsPassword(): Boolean =
    this is AppError.PasswordTooShort || this is AppError.WeakPassword || this is AppError.InvalidCredentials

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Composable
private fun AuthErrorTextPreview() {
    WandrTheme {
        Column(Modifier.padding(16.dp)) {
            AuthErrorText(AppError.InvalidCredentials())
            AuthErrorText(AppError.PasswordTooShort(6), Modifier.padding(top = 8.dp))
            AuthErrorText(AppError.EmailConfirmationRequired(), Modifier.padding(top = 8.dp))
            AuthErrorText(AppError.Network(), Modifier.padding(top = 8.dp))
        }
    }
}
