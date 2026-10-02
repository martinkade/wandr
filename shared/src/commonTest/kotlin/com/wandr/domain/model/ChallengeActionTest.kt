package com.wandr.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChallengeActionTest {

    private val individual = Challenge(
        "c1", "Open", null, null, "individual", "distance", 100_000.0, false, 0L, 1000L, "u1", 0L, 0L
    )
    private val group = individual.copy(id = "c2", scope = "group")

    private val joined = ChallengeParticipation("c1", teamId = null)
    private val enrolledTeam = ChallengeParticipation("c2", teamId = "t1")

    private fun action(
        challenge: Challenge,
        status: ChallengeStatus,
        participation: ChallengeParticipation? = null,
        hasTeams: Boolean = true
    ) = availableChallengeAction(challenge, status, participation, hasTeams)

    @Test
    fun joinIsOfferedForPlannedAndActiveChallenges() {
        assertEquals(ChallengeAction.JOIN, action(individual, ChallengeStatus.PLANNED))
        assertEquals(ChallengeAction.JOIN, action(individual, ChallengeStatus.ACTIVE))
    }

    @Test
    fun noJoinForDraftsAndFinishedChallenges() {
        assertNull(action(individual, ChallengeStatus.DRAFT))
        assertNull(action(individual, ChallengeStatus.EXPIRED))
        assertNull(action(individual, ChallengeStatus.COMPLETED))
    }

    @Test
    fun leaveReplacesJoinOnceJoined() {
        assertEquals(ChallengeAction.LEAVE, action(individual, ChallengeStatus.ACTIVE, joined))
        assertEquals(ChallengeAction.LEAVE, action(individual, ChallengeStatus.PLANNED, joined))
    }

    @Test
    fun noLeaveOnceTheChallengeIsOver() {
        assertNull(action(individual, ChallengeStatus.EXPIRED, joined))
        assertNull(action(individual, ChallengeStatus.COMPLETED, joined))
    }

    @Test
    fun aJoinedDraftCanStillBeLeft() {
        assertEquals(ChallengeAction.LEAVE, action(individual, ChallengeStatus.DRAFT, joined))
    }

    @Test
    fun groupChallengesOfferEnrollingOnlyToUsersWithATeam() {
        assertEquals(ChallengeAction.ENROLL_TEAM, action(group, ChallengeStatus.ACTIVE, hasTeams = true))
        assertNull(action(group, ChallengeStatus.ACTIVE, hasTeams = false))
        assertNull(action(group, ChallengeStatus.DRAFT, hasTeams = true))
        assertNull(action(group, ChallengeStatus.EXPIRED, hasTeams = true))
    }

    @Test
    fun enrolledTeamCanBeWithdrawnUntilTheChallengeIsOver() {
        assertEquals(ChallengeAction.WITHDRAW_TEAM, action(group, ChallengeStatus.ACTIVE, enrolledTeam))
        assertEquals(ChallengeAction.WITHDRAW_TEAM, action(group, ChallengeStatus.PLANNED, enrolledTeam, hasTeams = false))
        assertNull(action(group, ChallengeStatus.COMPLETED, enrolledTeam))
    }
}
