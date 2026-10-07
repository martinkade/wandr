package com.wandr.domain.error

/**
 * Everything that can go wrong in a way the user should hear about, independent of where it came from (our own input
 * checks, Supabase Auth, PostgREST, the network). Failures travel as `Result.failure(AppError)`; the UI picks a localized
 * text from the type, so raw server messages (URLs, headers, error codes) never reach the user. [message] is for logs.
 */
sealed class AppError(message: String, cause: Throwable? = null) : Exception(message, cause) {

    // --- Input, checked before any request --------------------------------------------------------------------

    class InvalidEmail(cause: Throwable? = null) : AppError("Invalid email address", cause)
    class PasswordTooShort(val minLength: Int = MIN_PASSWORD_LENGTH) :
        AppError("Password must have at least $minLength characters") {
        companion object {
            const val MIN_PASSWORD_LENGTH = 6
        }
    }

    class UsernameRequired : AppError("Username must not be blank")

    /** Other input that is not acceptable; [limit] is the maximum for the "too long / too large" problems (characters, bytes). */
    class InvalidInput(val problem: InputProblem, val limit: Int? = null) :
        AppError("Invalid input: $problem")

    // --- Sign in / sign up -----------------------------------------------------------------------------------

    class InvalidCredentials(cause: Throwable? = null) : AppError("Wrong email or password", cause)
    class EmailNotConfirmed(cause: Throwable? = null) :
        AppError("The email address is not confirmed yet", cause)

    /** Sign-up worked, but the account is only usable after the user confirmed the email address. Not a failure of the call. */
    class EmailConfirmationRequired :
        AppError("Sign-up succeeded; the email address must be confirmed first")

    class UserAlreadyExists(cause: Throwable? = null) :
        AppError("An account with this email exists already", cause)

    class WeakPassword(val reasons: List<String> = emptyList(), cause: Throwable? = null) :
        AppError("The password is too weak", cause)

    class SignupDisabled(cause: Throwable? = null) : AppError("Sign-up is disabled", cause)
    class UserBanned(cause: Throwable? = null) : AppError("This account is blocked", cause)

    /** The session is gone or no longer valid: the user has to sign in again. */
    class SessionExpired(cause: Throwable? = null) : AppError("The session expired", cause)

    // --- Any request ------------------------------------------------------------------------------------------

    /** No connection or the server could not be reached. */
    class Network(cause: Throwable? = null) : AppError("The server could not be reached", cause)
    class Timeout(cause: Throwable? = null) : AppError("The request took too long", cause)
    class RateLimited(cause: Throwable? = null) : AppError("Too many requests", cause)
    class PermissionDenied(cause: Throwable? = null) : AppError("Not allowed", cause)
    class NotFound(cause: Throwable? = null) : AppError("Not found", cause)

    /** The change collides with existing data (e.g. a unique value that is taken). */
    class Conflict(cause: Throwable? = null) : AppError("Conflicting data", cause)

    /** The server rejected the data as invalid. */
    class Validation(cause: Throwable? = null) : AppError("Invalid data", cause)
    class Server(val statusCode: Int? = null, cause: Throwable? = null) :
        AppError("Server error${statusCode?.let { " ($it)" }.orEmpty()}", cause)

    /** The app's own storage (database, files) could not be opened or written. */
    class LocalStorage(cause: Throwable? = null) :
        AppError("The local storage could not be used", cause)

    class Unknown(cause: Throwable? = null) : AppError(cause?.message ?: "Unexpected error", cause)

    /** True if trying the same call again later can help (as opposed to changing the input first). */
    val isRetryable: Boolean get() = this is Network || this is Timeout || this is Server || this is RateLimited

    companion object {
        /** Maps an error code of Supabase Auth (e.g. `invalid_credentials`) to an error; unknown codes fall back to the HTTP status. */
        fun fromAuthCode(
            code: String?,
            statusCode: Int?,
            cause: Throwable? = null,
            weakPasswordReasons: List<String> = emptyList()
        ): AppError =
            when (code) {
                "invalid_credentials" -> InvalidCredentials(cause)
                "email_not_confirmed" -> EmailNotConfirmed(cause)
                "user_already_exists", "email_exists" -> UserAlreadyExists(cause)
                "weak_password" -> WeakPassword(weakPasswordReasons, cause)
                "email_address_invalid", "email_address_not_authorized" -> InvalidEmail(cause)
                "signup_disabled", "email_provider_disabled" -> SignupDisabled(cause)
                "user_banned" -> UserBanned(cause)
                "session_expired", "session_not_found", "refresh_token_not_found", "refresh_token_already_used",
                "bad_jwt", "no_authorization" -> SessionExpired(cause)

                "over_request_rate_limit", "over_email_send_rate_limit" -> RateLimited(cause)
                "request_timeout" -> Timeout(cause)
                else -> fromStatus(statusCode, cause)
            }

        /** Maps an HTTP status of a failed request. */
        fun fromStatus(statusCode: Int?, cause: Throwable? = null): AppError = when {
            statusCode == null -> Unknown(cause)
            statusCode == 400 || statusCode == 422 -> Validation(cause)
            statusCode == 401 -> SessionExpired(cause)
            statusCode == 403 -> PermissionDenied(cause)
            statusCode == 404 -> NotFound(cause)
            statusCode == 408 -> Timeout(cause)
            statusCode == 409 -> Conflict(cause)
            statusCode == 429 -> RateLimited(cause)
            statusCode >= 500 -> Server(statusCode, cause)
            else -> Unknown(cause)
        }
    }
}

/** What is wrong with an input, for [AppError.InvalidInput]. Every value has a localized text on both platforms. */
enum class InputProblem {
    TITLE_REQUIRED,
    NAME_REQUIRED,
    DISPLAY_NAME_REQUIRED,
    TARGET_NOT_POSITIVE,
    END_BEFORE_START,
    NEGATIVE_VALUES,
    COMMENT_EMPTY,
    COMMENT_TOO_LONG,
    IMAGE_EMPTY,
    IMAGE_TOO_LARGE,
    INVITE_CODE_REQUIRED,
    INVITE_CODE_INVALID,
    UNSUPPORTED_REACTION,
    NOTHING_TO_ORDER,
    NOTHING_LEFT_TO_TRIM,
    WRONG_CHALLENGE_SCOPE
}

/** The failure as an [AppError]; anything else becomes [AppError.Unknown]. Safe to call on any [Throwable]. */
fun Throwable.asAppError(): AppError = this as? AppError ?: AppError.Unknown(this)
