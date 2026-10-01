package com.wandr.presentation.challenge

import com.wandr.domain.model.Challenge
import com.wandr.domain.usecase.CreateChallengeUseCase
import com.wandr.domain.usecase.GetChallengeLeaderboardUseCase
import com.wandr.domain.usecase.GetTeamChallengesUseCase
import com.wandr.domain.usecase.JoinChallengeUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

class ChallengeViewModel(
    private val getTeamChallengesUseCase: GetTeamChallengesUseCase,
    private val createChallengeUseCase: CreateChallengeUseCase,
    private val getChallengeLeaderboardUseCase: GetChallengeLeaderboardUseCase,
    private val joinChallengeUseCase: JoinChallengeUseCase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val _uiState = MutableStateFlow(ChallengeState())
    val uiState: StateFlow<ChallengeState> = _uiState.asStateFlow()

    fun processIntent(intent: ChallengeIntent) {
        when (intent) {
            is ChallengeIntent.LoadTeamChallenges -> loadTeamChallenges(intent.teamId)
            is ChallengeIntent.SelectChallenge -> selectChallenge(intent.challengeId, intent.teamId)
            is ChallengeIntent.TitleChanged -> _uiState.update { it.copy(createTitle = intent.title) }
            is ChallengeIntent.DescriptionChanged -> _uiState.update { it.copy(createDescription = intent.description) }
            is ChallengeIntent.TargetValueChanged -> _uiState.update { it.copy(createTargetValue = intent.value) }
            is ChallengeIntent.TypeChanged -> _uiState.update { it.copy(createType = intent.type) }
            is ChallengeIntent.RequireAllMembersCompletionChanged -> _uiState.update { it.copy(requireAllMembersCompletion = intent.requireAll) }
            is ChallengeIntent.SubmitCreateChallenge -> createChallenge(intent.teamId, intent.creatorId)
            is ChallengeIntent.JoinChallenge -> joinChallenge(intent.challengeId, intent.userId)
            is ChallengeIntent.ClearMessages -> _uiState.update { it.copy(errorMessage = null, successMessage = null) }
        }
    }

    private fun loadTeamChallenges(teamId: String) {
        _uiState.update { it.copy(isLoading = true) }
        scope.launch {
            getTeamChallengesUseCase(teamId).collect { challenges ->
                _uiState.update { it.copy(challenges = challenges, isLoading = false) }
            }
        }
    }

    private fun selectChallenge(challengeId: String, teamId: String) {
        _uiState.update { it.copy(isLoading = true) }
        scope.launch {
            getChallengeLeaderboardUseCase(challengeId, teamId).collect { leaderboard ->
                _uiState.update { it.copy(leaderboard = leaderboard, isLoading = false) }
            }
        }
    }

    private fun createChallenge(teamId: String, creatorId: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        val thirtyDays = 30 * 24 * 60 * 60 * 1000L
        val challenge = Challenge(
            id = "chal_$now",
            teamId = teamId,
            title = _uiState.value.createTitle,
            description = _uiState.value.createDescription,
            coverUrl = null,
            scope = "group",
            type = _uiState.value.createType,
            targetValue = _uiState.value.createTargetValue,
            requireAllMembersCompletion = _uiState.value.requireAllMembersCompletion,
            startDate = now,
            endDate = now + thirtyDays,
            status = "active",
            createdBy = creatorId,
            createdAt = now,
            updatedAt = now
        )

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        scope.launch {
            createChallengeUseCase(challenge)
                .onSuccess {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            successMessage = "Challenge created!",
                            createTitle = "",
                            createDescription = ""
                        ) 
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Creation failed") }
                }
        }
    }

    private fun joinChallenge(challengeId: String, userId: String) {
        scope.launch {
            joinChallengeUseCase(challengeId, userId)
                .onSuccess {
                    _uiState.update { it.copy(successMessage = "Joined Challenge!") }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "Failed to join") }
                }
        }
    }
}
