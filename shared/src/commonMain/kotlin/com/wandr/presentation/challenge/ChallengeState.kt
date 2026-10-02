package com.wandr.presentation.challenge

import com.wandr.domain.model.Challenge
import com.wandr.domain.model.ChallengeParticipation
import com.wandr.domain.model.ChallengeScope
import com.wandr.domain.model.ChallengeStatus
import com.wandr.domain.model.ChallengeType
import com.wandr.domain.model.LeaderboardEntry
import com.wandr.domain.model.Team
import com.wandr.domain.model.TeamStanding

enum class ChallengeSuccess { CREATED, UPDATED, JOINED, LEFT, TEAM_ENROLLED, TEAM_WITHDRAWN, IMAGE_UPDATED }

/** Content of the create / edit form. [challengeId] is null while creating. */
data class ChallengeForm(
    val challengeId: String? = null,
    val title: String = "",
    val description: String = "",
    val type: ChallengeType = ChallengeType.DISTANCE,
    /** Meters (distance, elevation) or seconds (time). */
    val targetValue: Double = 100_000.0,
    /** Epoch milliseconds; challenges can be planned for the future. */
    val startDate: Long = 0L,
    val endDate: Long = 0L,
    /** GROUP = teams compete against other teams; there is no team on the challenge itself. */
    val scope: ChallengeScope = ChallengeScope.INDIVIDUAL,
    val requireAllMembersCompletion: Boolean = false,
    /** Status toggle: true = `active` (published), false = `draft` (default, only the creator sees it). */
    val isActive: Boolean = false
) {
    val isEditing: Boolean get() = challengeId != null
}

data class ChallengeState(
    val challenges: List<Challenge> = emptyList(),
    /** Runtime status per challenge id, derived from the dates (see EvaluateChallengeStatusUseCase). */
    val statuses: Map<String, ChallengeStatus> = emptyMap(),
    /** The user's participations by challenge id; absent = not taking part. See [availableChallengeAction]. */
    val participations: Map<String, ChallengeParticipation> = emptyMap(),
    /** Teams the user belongs to; the choices when enrolling a team in a group challenge. */
    val teams: List<Team> = emptyList(),
    val selectedChallenge: Challenge? = null,
    /** Only the creator (owner) of the selected challenge may edit it and change its cover. */
    val canEdit: Boolean = false,
    /** Team vs. team ranking of the selected group challenge. */
    val standings: List<TeamStanding> = emptyList(),
    /** Progress of the user's own team members in the selected group challenge. */
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    /** Non-null while the create / edit sheet is open. */
    val form: ChallengeForm? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isImageUpdating: Boolean = false,
    val errorMessage: String? = null,
    val success: ChallengeSuccess? = null
)
