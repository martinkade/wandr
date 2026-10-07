package com.wandr.domain.error

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AppErrorTest {
    @Test
    fun authCodesBecomeTypedErrors() {
        assertIs<AppError.InvalidCredentials>(AppError.fromAuthCode("invalid_credentials", 400))
        assertIs<AppError.EmailNotConfirmed>(AppError.fromAuthCode("email_not_confirmed", 400))
        assertIs<AppError.UserAlreadyExists>(AppError.fromAuthCode("user_already_exists", 422))
        assertIs<AppError.UserAlreadyExists>(AppError.fromAuthCode("email_exists", 422))
        assertIs<AppError.InvalidEmail>(AppError.fromAuthCode("email_address_invalid", 400))
        assertIs<AppError.SignupDisabled>(AppError.fromAuthCode("signup_disabled", 422))
        assertIs<AppError.UserBanned>(AppError.fromAuthCode("user_banned", 403))
        assertIs<AppError.SessionExpired>(AppError.fromAuthCode("refresh_token_not_found", 401))
        assertIs<AppError.RateLimited>(AppError.fromAuthCode("over_email_send_rate_limit", 429))
        assertIs<AppError.Timeout>(AppError.fromAuthCode("request_timeout", 504))
    }

    @Test
    fun weakPasswordKeepsItsReasons() {
        val error = assertIs<AppError.WeakPassword>(
            AppError.fromAuthCode(
                "weak_password",
                422,
                weakPasswordReasons = listOf("length", "characters")
            )
        )
        assertEquals(listOf("length", "characters"), error.reasons)
    }

    @Test
    fun anUnknownAuthCodeFallsBackToTheHttpStatus() {
        assertIs<AppError.Server>(AppError.fromAuthCode("something_new", 503))
        assertIs<AppError.Validation>(AppError.fromAuthCode("validation_failed", 422))
        assertIs<AppError.Unknown>(AppError.fromAuthCode(null, null))
    }

    @Test
    fun httpStatusesBecomeTypedErrors() {
        assertIs<AppError.Validation>(AppError.fromStatus(400))
        assertIs<AppError.SessionExpired>(AppError.fromStatus(401))
        assertIs<AppError.PermissionDenied>(AppError.fromStatus(403)) // e.g. a row level security violation
        assertIs<AppError.NotFound>(AppError.fromStatus(404))
        assertIs<AppError.Timeout>(AppError.fromStatus(408))
        assertIs<AppError.Conflict>(AppError.fromStatus(409)) // e.g. a unique constraint
        assertIs<AppError.RateLimited>(AppError.fromStatus(429))
        assertEquals(502, assertIs<AppError.Server>(AppError.fromStatus(502)).statusCode)
        assertIs<AppError.Unknown>(AppError.fromStatus(418))
        assertIs<AppError.Unknown>(AppError.fromStatus(null))
    }

    @Test
    fun theCauseIsKeptForLogging() {
        val cause = IllegalStateException("boom")
        assertSame(cause, AppError.fromStatus(500, cause).cause)
    }

    @Test
    fun onlyTransientProblemsAreRetryable() {
        assertTrue(AppError.Network().isRetryable)
        assertTrue(AppError.Timeout().isRetryable)
        assertTrue(AppError.Server(500).isRetryable)
        assertTrue(AppError.RateLimited().isRetryable)
        assertFalse(AppError.InvalidCredentials().isRetryable)
        assertFalse(AppError.PermissionDenied().isRetryable)
    }

    @Test
    fun anyThrowableCanBeViewedAsAnAppError() {
        val known = AppError.Network()
        assertSame(known, known.asAppError())
        assertIs<AppError.Unknown>(IllegalArgumentException("x").asAppError())
    }
}
