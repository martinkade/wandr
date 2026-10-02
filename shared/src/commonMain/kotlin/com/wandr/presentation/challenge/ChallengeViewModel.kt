package com.wandr.presentation.challenge

import com.wandr.domain.model.Challenge
import com.wandr.domain.model.ChallengeScope
import com.wandr.domain.model.ChallengeType
import com.wandr.domain.usecase.CreateChallengeUseCase
import com.wandr.domain.usecase.GetChallengeLeaderboardUseCase
import com.wandr.domain.usecase.EnrollTeamInChallengeUseCase
import com.wandr.domain.usecase.EvaluateChallengeStatusUseCase
import com.wandr.domain.usecase.GetChallengeUseCase
import com.wandr.domain.usecase.GetChallengesUseCase
import com.wandr.domain.usecase.RemoveChallengeCoverUseCase
import com.wandr.domain.usecase.SetChallengeCoverUseCase
import com.wandr.domain.usecase.GetTeamStandingsUseCase
import com.wandr.domain.usecase.GetUserTeamsUseCase
import com.wandr.domain.usecase.JoinChallengeUseCase
import com.wandr.domain.usecase.RefreshChallengesUseCase
import com.wandr.domain.usecase.UpdateChallengeUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ChallengeViewModel(
    private val getChallengesUseCase: GetChallengesUseCase,
    private val getChallengeUseCase: GetChallengeUseCase,
    private val setChallengeCoverUseCase: SetChallengeCoverUseCase,
    private val removeChallengeCoverUseCase: RemoveChallengeCoverUseCase,
    private val evaluateChallengeStatusUseCase: EvaluateChallengeStatusUseCase,
    private val refreshChallengesUseCase: RefreshChallengesUseCase,
    private val getUserTeamsUseCase: GetUserTeamsUseCase,
    private val createChallengeUseCase: CreateChallengeUseCase,
    private val updateChallengeUseCase: UpdateChallengeUseCase,
    private val getChallengeLeaderboardUseCase: GetChallengeLeaderboardUseCase,
    private val getTeamStandingsUseCase: GetTeamStandingsUseCase,
    private val enrollTeamInChallengeUseCase: EnrollTeamInChallengeUseCase,
    private val joinChallengeUseCase: JoinChallengeUseCase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val _uiState = MutableStateFlow(ChallengeState())
    val uiState: StateFlow<ChallengeState> = _uiState.asStateFlow()

    private var observeJob: Job? = null
    private var selectJob: Job? = null

    fun processIntent(intent: ChallengeIntent) {
        when (intent) {
            is ChallengeIntent.LoadChallenges -> load(intent.userId)
            is ChallengeIntent.SelectChallenge -> selectChallenge(intent.challengeId, intent.teamId)
            is ChallengeIntent.StartCreate -> startCreate()
            is ChallengeIntent.StartEdit -> startEdit(intent.challengeId)
            is ChallengeIntent.TitleChanged -> updateForm { it.copy(title = intent.title) }
            is ChallengeIntent.DescriptionChanged -> updateForm { it.copy(description = intent.description) }
            is ChallengeIntent.TypeChanged -> updateForm { form ->
                // Keep a sensible magnitude when the unit changes (100 km -> 100 m would be silly).
                form.copy(type = intent.type, targetValue = defaultTarget(intent.type))
            }
            is ChallengeIntent.TargetValueChanged -> updateForm { it.copy(targetValue = intent.value) }
            is ChallengeIntent.StartDateChanged -> updateForm { form ->
                val length = (form.endDate - form.startDate).takeIf { it > 0 } ?: DAY_MILLIS
                form.copy(
                    startDate = intent.millis,
                    endDate = if (form.endDate <= intent.millis) intent.millis + length else form.endDate
                )
            }
            is ChallengeIntent.EndDateChanged -> updateForm { it.copy(endDate = intent.millis) }
            is ChallengeIntent.ActiveChanged -> updateForm { it.copy(isActive = intent.isActive) }
            is ChallengeIntent.ScopeChanged -> updateForm { form ->
                form.copy(
                    scope = intent.scope,
                    requireAllMembersCompletion = form.requireAllMembersCompletion && intent.scope == ChallengeScope.GROUP
                )
            }
            is ChallengeIntent.RequireAllMembersCompletionChanged -> updateForm { it.copy(requireAllMembersCompletion = intent.requireAll) }
            is ChallengeIntent.SubmitForm -> submit(intent.userId)
            is ChallengeIntent.DiscardForm -> _uiState.update { it.copy(form = null) }
            is ChallengeIntent.UploadCover -> updateCover { id -> setChallengeCoverUseCase(id, intent.jpegBytes) }
            is ChallengeIntent.RemoveCover -> updateCover { id -> removeChallengeCoverUseCase(id) }
            is ChallengeIntent.JoinChallenge -> joinChallenge(intent.challengeId, intent.userId)
            is ChallengeIntent.EnrollTeam -> enrollTeam(intent.challengeId, intent.teamId, intent.userId)
            is ChallengeIntent.ClearMessages -> _uiState.update { it.copy(errorMessage = null, success = null) }
        }
    }

    private fun load(userId: String) {
        _uiState.update { it.copy(isLoading = it.challenges.isEmpty()) }
        observeJob?.cancel()
        observeJob = scope.launch {
            // The local cache drives the UI; the remote pull only feeds it (and may fail while offline).
            launch { refreshChallengesUseCase() }
            launch { getUserTeamsUseCase(userId).collect { teams -> _uiState.update { it.copy(teams = teams) } } }
            getChallengesUseCase().collect { challenges ->
                val now = Clock.System.now().toEpochMilliseconds()
                val statuses = challenges.associate { it.id to evaluateChallengeStatusUseCase(it, currentTimeMillis = now) }
                _uiState.update { it.copy(challenges = challenges, statuses = statuses, isLoading = false) }
            }
        }
    }

    private fun selectChallenge(challengeId: String, teamId: String?) {
        _uiState.update { it.copy(isLoading = true, standings = emptyList(), leaderboard = emptyList()) }
        selectJob?.cancel()
        selectJob = scope.launch {
            launch {
                getChallengeUseCase(challengeId).collect { challenge ->
                    _uiState.update { it.copy(selectedChallenge = challenge) }
                }
            }
            launch {
                // Team vs. team ranking (aggregates, from the server); may fail while offline.
                getTeamStandingsUseCase(challengeId)
                    .onSuccess { standings -> _uiState.update { it.copy(standings = standings, isLoading = false) } }
                    .onFailure { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message) } }
            }
            if (teamId != null) {
                getChallengeLeaderboardUseCase(challengeId, teamId).collect { leaderboard ->
                    _uiState.update { it.copy(leaderboard = leaderboard) }
                }
            }
        }
    }

    private fun startCreate() {
        // Whole minutes are enough for a start time; the end defaults to 30 days later.
        val now = Clock.System.now().toEpochMilliseconds().let { it - it % MINUTE_MILLIS }
        _uiState.update {
            it.copy(form = ChallengeForm(startDate = now, endDate = now + 30 * DAY_MILLIS), errorMessage = null)
        }
    }

    private fun startEdit(challengeId: String) {
        val state = _uiState.value
        val challenge = state.challenges.firstOrNull { it.id == challengeId }
            ?: state.selectedChallenge?.takeIf { it.id == challengeId }
            ?: return
        _uiState.update {
            it.copy(
                errorMessage = null,
                form = ChallengeForm(
                    challengeId = challenge.id,
                    title = challenge.title,
                    description = challenge.description.orEmpty(),
                    type = ChallengeType.fromValue(challenge.type),
                    targetValue = challenge.targetValue,
                    startDate = challenge.startDate,
                    endDate = challenge.endDate,
                    scope = ChallengeScope.fromValue(challenge.scope),
                    requireAllMembersCompletion = challenge.requireAllMembersCompletion,
                    isActive = challenge.isActive
                )
            )
        }
    }

    private fun updateForm(transform: (ChallengeForm) -> ChallengeForm) {
        _uiState.update { state -> state.copy(form = state.form?.let(transform)) }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun submit(userId: String) {
        val form = _uiState.value.form ?: return
        val now = Clock.System.now().toEpochMilliseconds()
        val state = _uiState.value
        val existing = form.challengeId?.let { id ->
            state.challenges.firstOrNull { it.id == id } ?: state.selectedChallenge?.takeIf { it.id == id }
        }
        if (form.isEditing && existing == null) return

        val challenge = Challenge(
            id = existing?.id ?: Uuid.random().toString(),
            title = form.title,
            description = form.description.trim().ifEmpty { null },
            coverUrl = existing?.coverUrl,
            scope = form.scope.value,
            type = form.type.value,
            targetValue = form.targetValue,
            requireAllMembersCompletion = form.scope == ChallengeScope.GROUP && form.requireAllMembersCompletion,
            startDate = form.startDate,
            endDate = form.endDate,
            createdBy = existing?.createdBy ?: userId,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            isActive = form.isActive
        )

        _uiState.update { it.copy(isSaving = true, errorMessage = null, success = null) }
        scope.launch {
            val result = if (existing == null) createChallengeUseCase(challenge) else updateChallengeUseCase(challenge)
            result
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            success = if (existing == null) ChallengeSuccess.CREATED else ChallengeSuccess.UPDATED
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = error.message ?: "Saving failed") }
                }
        }
    }

    private fun updateCover(action: suspend (challengeId: String) -> Result<Challenge>) {
        val challengeId = _uiState.value.selectedChallenge?.id ?: return
        _uiState.update { it.copy(isImageUpdating = true, errorMessage = null, success = null) }
        scope.launch {
            action(challengeId)
                .onSuccess { _uiState.update { it.copy(isImageUpdating = false, success = ChallengeSuccess.IMAGE_UPDATED) } }
                .onFailure { error ->
                    _uiState.update { it.copy(isImageUpdating = false, errorMessage = error.message ?: "Image update failed") }
                }
        }
    }

    private fun defaultTarget(type: ChallengeType): Double = when (type) {
        ChallengeType.DISTANCE -> 100_000.0 // 100 km
        ChallengeType.ELEVATION -> 5_000.0 // 5000 m
        ChallengeType.TIME -> 36_000.0 // 10 h
    }

    private fun enrollTeam(challengeId: String, teamId: String, userId: String) {
        scope.launch {
            enrollTeamInChallengeUseCase(challengeId, teamId, userId)
                .onSuccess { _uiState.update { it.copy(success = ChallengeSuccess.TEAM_ENROLLED) } }
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message ?: "Could not enroll the team") } }
        }
    }

    private fun joinChallenge(challengeId: String, userId: String) {
        scope.launch {
            joinChallengeUseCase(challengeId, userId)
                .onSuccess { _uiState.update { it.copy(success = ChallengeSuccess.JOINED) } }
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message ?: "Failed to join") } }
        }
    }

    private companion object {
        const val MINUTE_MILLIS = 60_000L
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
