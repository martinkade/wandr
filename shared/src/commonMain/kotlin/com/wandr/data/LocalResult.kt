package com.wandr.data

import com.wandr.domain.error.asAppError
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs a local operation (database, files) and returns its result, with failures as [com.wandr.domain.error.AppError].
 * Like `supabaseResult`, it never swallows a cancellation.
 */
internal inline fun <T> localResult(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Throwable) {
    Result.failure(error.asAppError())
}
