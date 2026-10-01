package com.wandr.presentation.challenge

import com.wandr.domain.model.Challenge
import com.wandr.domain.model.LeaderboardEntry

data class ChallengeState(
    val challenges: List<Challenge> = emptyList(),
    val selectedChallenge: Challenge? = null,
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val createTitle: String = "",
    val createDescription: String = "",
    val createTargetValue: Double = 100000.0,
    val createType: String = "distance", // distance, elevation, time
    val requireAllMembersCompletion: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
