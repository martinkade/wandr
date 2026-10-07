package com.wandr.data.remote

import com.wandr.domain.error.AppError
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.exception.AuthSessionMissingException
import io.github.jan.supabase.auth.exception.AuthWeakPasswordException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import kotlin.coroutines.cancellation.CancellationException

/** Turns what the Supabase SDK throws into an [AppError]. */
object SupabaseErrorMapper {

    fun map(error: Throwable): AppError = when {
        error is AppError -> error
        error is AuthWeakPasswordException -> AppError.fromAuthCode(
            error.error,
            error.statusCode,
            error,
            error.reasons
        )

        error is AuthSessionMissingException -> AppError.SessionExpired(error)
        error is AuthRestException -> AppError.fromAuthCode(error.error, error.statusCode, error)
        error is RestException -> AppError.fromStatus(error.statusCode, error)
        // The SDK reports connectivity problems as an IOException subclass.
        error is HttpRequestException -> if (isTimeout(error)) AppError.Timeout(error) else AppError.Network(
            error
        )

        isTimeout(error) -> AppError.Timeout(error)
        isNetwork(error) -> AppError.Network(error)
        else -> AppError.Unknown(error)
    }

    /** Ktor and the platform HTTP engines name their timeout exceptions alike; the cause chain is searched. */
    private fun isTimeout(error: Throwable): Boolean =
        causes(error).any { it::class.simpleName?.contains("Timeout", ignoreCase = true) == true }

    private fun isNetwork(error: Throwable): Boolean = causes(error).any {
        val name = it::class.simpleName.orEmpty()
        name.contains("UnknownHost") || name.contains("UnresolvedAddress") || name.contains("ConnectException") ||
                name.contains("SocketException") || name.contains("SSLException") || name == "IOException"
    }

    private fun causes(error: Throwable): Sequence<Throwable> =
        generateSequence(error) { it.cause?.takeIf { cause -> cause !== it } }.take(MAX_CAUSE_DEPTH)

    private const val MAX_CAUSE_DEPTH = 6
}

/**
 * Runs a Supabase call and returns its result, with failures mapped to [AppError]. Unlike `runCatching` it never
 * swallows a cancellation: a cancelled coroutine has to stop, not turn into an error message.
 */
internal inline fun <T> supabaseResult(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Throwable) {
    Result.failure(SupabaseErrorMapper.map(error))
}
