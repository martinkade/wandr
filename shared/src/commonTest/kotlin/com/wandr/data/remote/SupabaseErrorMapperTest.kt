package com.wandr.data.remote

import com.wandr.domain.error.AppError
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.request.HttpRequestBuilder
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

private class SocketTimeoutException(message: String) : Exception(message)
private class UnknownHostException(message: String) : Exception(message)

class SupabaseErrorMapperTest {
    @Test
    fun connectivityProblemsOfTheSdkAreNetworkErrors() {
        assertIs<AppError.Network>(
            SupabaseErrorMapper.map(
                HttpRequestException(
                    "no route to host",
                    HttpRequestBuilder()
                )
            )
        )
    }

    @Test
    fun timeoutsAreFoundInTheCauseChain() {
        val wrapped = RuntimeException("wrapper", SocketTimeoutException("read timed out"))
        assertIs<AppError.Timeout>(SupabaseErrorMapper.map(wrapped))
    }

    @Test
    fun unresolvedHostsAreNetworkErrors() {
        assertIs<AppError.Network>(SupabaseErrorMapper.map(UnknownHostException("no such host")))
    }

    @Test
    fun appErrorsPassThroughAndUnknownThingsAreWrapped() {
        val known = AppError.InvalidEmail()
        assertEquals(known, SupabaseErrorMapper.map(known))
        val unknown = SupabaseErrorMapper.map(IllegalStateException("boom"))
        assertIs<AppError.Unknown>(unknown)
        assertEquals("boom", unknown.cause?.message)
    }

    @Test
    fun supabaseResultMapsFailuresButNeverSwallowsACancellation() = runTest {
        val failed =
            supabaseResult<Unit> { throw HttpRequestException("offline", HttpRequestBuilder()) }
        assertIs<AppError.Network>(failed.exceptionOrNull())
        assertEquals(42, supabaseResult { 42 }.getOrNull())

        assertFailsWith<CancellationException> {
            supabaseResult<Unit> {
                throw CancellationException(
                    "cancelled"
                )
            }
        }
    }
}
