package com.wandr.presentation.challenge

import com.wandr.domain.model.Challenge
import com.wandr.domain.model.ChallengeScope
import com.wandr.domain.model.ChallengeStatus
import com.wandr.domain.model.ChallengeType
import com.wandr.domain.model.LeaderboardEntry
import com.wandr.domain.model.Team
import com.wandr.domain.model.TeamStanding

enum class ChallengeSuccess { CREATED, UPDATED, JOINED, TEAM_ENROLLED }

/** Content of the create / edit form. [challengeId] is null while creating. */
data class ChallengeForm(
    val challengeId: String? = null,
    val title: String = "",
    val description: String = "",
    val type: ChallengeType = ChallengeType.DISTANCE,
    /** Meters (distance, elevation) or seconds (time). */
    val targetValue: Double = 100_000.0,
    val durationDays: Int = 30,
    /** GROUP = teams compete against other teams; there is no team on the challenge itself. */
    val scope: ChallengeScope = ChallengeScope.INDIVIDUAL,
    val requireAllMembersCompletion: Boolean = false
) {
    val isEditing: Boolean get() = challengeId != null
}

data class ChallengeState(
    val challenges: List<Challenge> = emptyList(),
    /** Runtime status per challenge id, derived from the dates (see EvaluateChallengeStatusUseCase). */
    val statuses: Map<String, ChallengeStatus> = emptyMap(),
    /** Teams the user belongs to; the choices when enrolling a team in a group challenge. */
    val teams: List<Team> = emptyList(),
    val selectedChallenge: Challenge? = null,
    /** Team vs. team ranking of the selected group challenge. */
    val standings: List<TeamStanding> = emptyList(),
    /** Progress of the user's own team members in the selected group challenge. */
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    /** Non-null while the create / edit sheet is open. */
    val form: ChallengeForm? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val success: ChallengeSuccess? = null
)
