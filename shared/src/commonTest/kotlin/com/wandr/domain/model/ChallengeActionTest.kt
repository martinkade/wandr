package com.wandr.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChallengeActionTest {

    private val individual = Challenge(
        "c1", "Open", null, null, "individual", "distance", 100_000.0, false, 0L, 1000L, "u1", 0L, 0L
    )
    private val teamChallenge = individual.copy(id = "c2", scope = "group")

    private val joined = ChallengeParticipation("c1", teamId = null)
    private val enrolledTeam = ChallengeParticipation("c2", teamId = "t1")

    private fun action(
        challenge: Challenge,
        status: ChallengeStatus,
        participation: ChallengeParticipation? = null,
        admin: Set<String> = emptySet(),
        enrolled: Set<String> = emptySet()
    ) = availableChallengeAction(challenge, status, participation, admin, enrolled)

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
    fun teamChallengesOfferEnrollingOnlyToTeamAdmins() {
        assertEquals(
            ChallengeAction.ENROLL_TEAM,
            action(teamChallenge, ChallengeStatus.ACTIVE, admin = setOf("t1"))
        )
        assertEquals(
            ChallengeAction.ENROLL_TEAM,
            action(teamChallenge, ChallengeStatus.PLANNED, admin = setOf("t1"))
        )
        // a default member (no administered team) sees no button
        assertNull(action(teamChallenge, ChallengeStatus.ACTIVE, admin = emptySet()))
        assertNull(action(teamChallenge, ChallengeStatus.DRAFT, admin = setOf("t1")))
        assertNull(action(teamChallenge, ChallengeStatus.EXPIRED, admin = setOf("t1")))
    }

    @Test
    fun aTeamThatIsEnrolledAlreadyIsNotOfferedAgain() {
        assertNull(
            action(
                teamChallenge,
                ChallengeStatus.ACTIVE,
                admin = setOf("t1"),
                enrolled = setOf("t1")
            ).takeIf { it == ChallengeAction.ENROLL_TEAM })
        // an admin of two teams can still enroll the second one
        assertEquals(
            ChallengeAction.WITHDRAW_TEAM,
            action(
                teamChallenge,
                ChallengeStatus.ACTIVE,
                admin = setOf("t1", "t2"),
                enrolled = setOf("t1")
            )
        )
    }

    @Test
    fun onlyAdminsMayWithdrawAnEnrolledTeamAndOnlyUntilItIsOver() {
        assertEquals(
            ChallengeAction.WITHDRAW_TEAM,
            action(
                teamChallenge,
                ChallengeStatus.ACTIVE,
                admin = setOf("t1"),
                enrolled = setOf("t1")
            )
        )
        assertEquals(
            ChallengeAction.WITHDRAW_TEAM,
            action(
                teamChallenge,
                ChallengeStatus.PLANNED,
                admin = setOf("t1"),
                enrolled = setOf("t1")
            )
        )
        assertNull(
            action(
                teamChallenge,
                ChallengeStatus.COMPLETED,
                admin = setOf("t1"),
                enrolled = setOf("t1")
            )
        )
        // a default member of the enrolled team (participates, but does not administer it) sees nothing
        assertNull(
            action(
                teamChallenge,
                ChallengeStatus.ACTIVE,
                enrolledTeam,
                admin = emptySet(),
                enrolled = setOf("t1")
            )
        )
        // an admin of another team cannot withdraw a team they do not administer
        assertEquals(
            ChallengeAction.ENROLL_TEAM,
            action(
                teamChallenge,
                ChallengeStatus.ACTIVE,
                admin = setOf("t9"),
                enrolled = setOf("t1")
            )
        )
    }
}
