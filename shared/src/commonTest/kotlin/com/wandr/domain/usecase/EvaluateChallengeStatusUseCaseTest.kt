package com.wandr.domain.usecase

import com.wandr.domain.model.Challenge
import kotlin.test.Test
import kotlin.test.assertEquals

class EvaluateChallengeStatusUseCaseTest {

    private val evaluator = EvaluateChallengeStatusUseCase()

    private val sampleChallenge = Challenge(
        id = "c1",
        teamId = "t1",
        title = "100km Hike",
        description = "Group challenge",
        coverUrl = null,
        scope = "group",
        type = "distance",
        targetValue = 100000.0,
        requireAllMembersCompletion = true,
        startDate = 1000L,
        endDate = 5000L,
        status = "active",
        createdBy = "user1",
        createdAt = 1000L,
        updatedAt = 1000L
    )

    @Test
    fun testAllMembersRequiredCompletedOnlyWhenAllFinish() {
        // 2 out of 3 members finished before end date -> Active
        val activeStatus = evaluator(
            challenge = sampleChallenge,
            totalParticipantsCount = 3,
            completedParticipantsCount = 2,
            currentTimeMillis = 3000L
        )
        assertEquals("active", activeStatus)

        // All 3 finished before end date -> Completed
        val completedStatus = evaluator(
            challenge = sampleChallenge,
            totalParticipantsCount = 3,
            completedParticipantsCount = 3,
            currentTimeMillis = 3000L
        )
        assertEquals("completed", completedStatus)
    }

    @Test
    fun testAllMembersRequiredExpiresIfOneFails() {
        // 2 out of 3 finished after end date -> Expired (Failed)
        val expiredStatus = evaluator(
            challenge = sampleChallenge,
            totalParticipantsCount = 3,
            completedParticipantsCount = 2,
            currentTimeMillis = 6000L
        )
        assertEquals("expired", expiredStatus)
    }
}
