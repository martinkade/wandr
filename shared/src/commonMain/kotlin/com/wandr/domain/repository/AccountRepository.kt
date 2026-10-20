package com.wandr.domain.repository

/** Wiping the user's data or the whole account, on the server and in this app's cache. */
interface AccountRepository {
    /**
     * Removes everything the user created or did (activities, likes, comments, reactions, the teams and challenges they
     * created, participations, memberships, notifications) from the server and the local cache. The account and the
     * profile stay; the user stays signed in.
     */
    suspend fun resetData(userId: String): Result<Unit>

    /** [resetData], then deletes the profile and the account itself and signs out on this device. */
    suspend fun deleteAccount(userId: String): Result<Unit>
}
