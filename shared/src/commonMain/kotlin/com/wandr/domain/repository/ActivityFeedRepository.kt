package com.wandr.domain.repository

/** Pulls activities from the server into the local cache (`ActivityRepository` then serves them as usual). */
interface ActivityFeedRepository {
    /** The activities of all members of [teamId] (the group feed). */
    suspend fun refreshTeam(teamId: String): Result<Unit>

    /** The user's own activities, e.g. recorded on another device. */
    suspend fun refreshUser(userId: String): Result<Unit>
}
