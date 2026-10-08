package com.wandr.domain.usecase

import com.wandr.domain.model.Challenge
import com.wandr.domain.model.ChallengeStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class EvaluateChallengeStatusUseCaseTest {

    private val evaluator = EvaluateChallengeStatusUseCase()

    private val teamChallenge = Challenge(
        id = "c1",
        title = "100km Hike",
        description = "Team challenge",
        coverUrl = null,
        scope = "group",
        type = "distance",
        targetValue = 100000.0,
        requireAllMembersCompletion = true,
        startDate = 1000L,
        endDate = 5000L,
        createdBy = "user1",
        createdAt = 1000L,
        updatedAt = 1000L
    )

    private val openChallenge =
        teamChallenge.copy(scope = "individual", requireAllMembersCompletion = false)

    @Test
    fun draft_whenNotPublished_regardlessOfTheDates() {
        val draft = teamChallenge.copy(isActive = false)
        assertEquals(ChallengeStatus.DRAFT, evaluator(draft, currentTimeMillis = 999L))
        assertEquals(ChallengeStatus.DRAFT, evaluator(draft, currentTimeMillis = 3000L))
        assertEquals(ChallengeStatus.DRAFT, evaluator(draft, currentTimeMillis = 9000L))
    }

    @Test
    fun planned_whenPublishedButNotStartedYet() {
        assertEquals(ChallengeStatus.PLANNED, evaluator(teamChallenge, currentTimeMillis = 999L))
    }

    @Test
    fun active_betweenStartAndEndWithoutParticipantData() {
        assertEquals(ChallengeStatus.ACTIVE, evaluator(teamChallenge, currentTimeMillis = 1000L))
        assertEquals(ChallengeStatus.ACTIVE, evaluator(teamChallenge, currentTimeMillis = 5000L))
    }

    @Test
    fun expired_afterEndDateWithoutCompletion() {
        assertEquals(ChallengeStatus.EXPIRED, evaluator(teamChallenge, currentTimeMillis = 5001L))
    }

    @Test
    fun allMembersRequired_completedOnlyWhenAllFinish() {
        assertEquals(
            ChallengeStatus.ACTIVE,
            evaluator(
                teamChallenge,
                totalParticipantsCount = 3,
                completedParticipantsCount = 2,
                currentTimeMillis = 3000L
            )
        )
        assertEquals(
            ChallengeStatus.COMPLETED,
            evaluator(
                teamChallenge,
                totalParticipantsCount = 3,
                completedParticipantsCount = 3,
                currentTimeMillis = 3000L
            )
        )
    }

    @Test
    fun allMembersRequired_expiresIfOneFails() {
        assertEquals(
            ChallengeStatus.EXPIRED,
            evaluator(
                teamChallenge,
                totalParticipantsCount = 3,
                completedParticipantsCount = 2,
                currentTimeMillis = 6000L
            )
        )
    }

    @Test
    fun anyMemberCompleting_completesChallengeWithoutAllRequired() {
        assertEquals(
            ChallengeStatus.COMPLETED,
            evaluator(openChallenge, totalParticipantsCount = 3, completedParticipantsCount = 1, currentTimeMillis = 3000L)
        )
    }

    @Test
    fun onlyDraftAndActiveAreStored() {
        assertEquals("draft", ChallengeStatus.storedValue(isActive = false))
        assertEquals("active", ChallengeStatus.storedValue(isActive = true))
    }
}
