package com.wandr.domain.repository

/** Pulls activities from the server into the local cache (`ActivityRepository` then serves them as usual). */
interface ActivityFeedRepository {
    /**
     * The activities of all members of [teamId] (the team feed), with the routes the owners share on the map and the
     * profiles of the authors. [userId] is the signed-in user, whose own routes are never dropped.
     */
    suspend fun refreshTeam(teamId: String, userId: String): Result<Unit>

    /** The user's own activities, e.g. recorded on another device. */
    suspend fun refreshUser(userId: String): Result<Unit>
}
