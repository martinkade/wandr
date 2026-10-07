package com.wandr.android.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wandr.android.R
import com.wandr.domain.error.AppError
import com.wandr.domain.error.InputProblem

/**
 * The localized text for an [AppError], for the user: what happened and, where it helps, what to do. Raw server texts
 * (URLs, headers, error codes) are never shown; they stay in the error's cause for logs.
 */
@Composable
fun AppError.userMessage(): String = when (this) {
    is AppError.InvalidEmail -> stringResource(R.string.error_invalid_email)
    is AppError.PasswordTooShort -> stringResource(R.string.error_password_too_short, minLength)
    is AppError.UsernameRequired -> stringResource(R.string.error_username_required)
    is AppError.InvalidInput -> when (problem) {
        InputProblem.TITLE_REQUIRED -> stringResource(R.string.error_input_title_required)
        InputProblem.NAME_REQUIRED -> stringResource(R.string.error_input_name_required)
        InputProblem.DISPLAY_NAME_REQUIRED -> stringResource(R.string.error_input_display_name_required)
        InputProblem.TARGET_NOT_POSITIVE -> stringResource(R.string.error_input_target_not_positive)
        InputProblem.END_BEFORE_START -> stringResource(R.string.error_input_end_before_start)
        InputProblem.NEGATIVE_VALUES -> stringResource(R.string.error_input_negative_values)
        InputProblem.COMMENT_EMPTY -> stringResource(R.string.error_input_comment_empty)
        InputProblem.COMMENT_TOO_LONG -> stringResource(
            R.string.error_input_comment_too_long,
            limit ?: 0
        )

        InputProblem.IMAGE_EMPTY -> stringResource(R.string.error_input_image_empty)
        // The limit is in bytes; people think in megabytes.
        InputProblem.IMAGE_TOO_LARGE -> stringResource(
            R.string.error_input_image_too_large,
            (limit ?: 0) / (1024 * 1024)
        )

        InputProblem.INVITE_CODE_REQUIRED -> stringResource(R.string.error_input_invite_code_required)
        InputProblem.INVITE_CODE_INVALID -> stringResource(R.string.error_input_invite_code_invalid)
        InputProblem.UNSUPPORTED_REACTION -> stringResource(R.string.error_input_unsupported_reaction)
        InputProblem.NOTHING_TO_ORDER -> stringResource(R.string.error_input_nothing_to_order)
        InputProblem.NOTHING_LEFT_TO_TRIM -> stringResource(R.string.error_input_nothing_left_to_trim)
        InputProblem.WRONG_CHALLENGE_SCOPE -> stringResource(R.string.error_input_wrong_challenge_scope)
    }

    is AppError.InvalidCredentials -> stringResource(R.string.error_invalid_credentials)
    is AppError.EmailNotConfirmed -> stringResource(R.string.error_email_not_confirmed)
    is AppError.EmailConfirmationRequired -> stringResource(R.string.error_email_confirmation_required)
    is AppError.UserAlreadyExists -> stringResource(R.string.error_user_already_exists)
    is AppError.WeakPassword -> stringResource(R.string.error_weak_password)
    is AppError.SignupDisabled -> stringResource(R.string.error_signup_disabled)
    is AppError.UserBanned -> stringResource(R.string.error_user_banned)
    is AppError.SessionExpired -> stringResource(R.string.error_session_expired)
    is AppError.Network -> stringResource(R.string.error_network)
    is AppError.Timeout -> stringResource(R.string.error_timeout)
    is AppError.RateLimited -> stringResource(R.string.error_rate_limited)
    is AppError.PermissionDenied -> stringResource(R.string.error_permission_denied)
    is AppError.NotFound -> stringResource(R.string.error_not_found)
    is AppError.Conflict -> stringResource(R.string.error_conflict)
    is AppError.Validation -> stringResource(R.string.error_validation)
    is AppError.Server -> stringResource(R.string.error_server)
    is AppError.LocalStorage -> stringResource(R.string.error_local_storage)
    is AppError.Unknown -> stringResource(R.string.error_unknown)
}
