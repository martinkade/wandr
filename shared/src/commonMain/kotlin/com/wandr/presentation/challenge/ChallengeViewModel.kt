package com.wandr.presentation.challenge

import com.wandr.domain.error.AppError
import com.wandr.domain.error.asAppError
import com.wandr.domain.model.Challenge
import com.wandr.domain.model.ChallengeScope
import com.wandr.domain.model.ChallengeType
import com.wandr.domain.usecase.CreateChallengeUseCase
import com.wandr.domain.usecase.EnrollTeamInChallengeUseCase
import com.wandr.domain.usecase.EvaluateChallengeStatusUseCase
import com.wandr.domain.usecase.GetAdminTeamsUseCase
import com.wandr.domain.usecase.GetChallengeParticipationsUseCase
import com.wandr.domain.usecase.GetChallengeUseCase
import com.wandr.domain.usecase.GetChallengesUseCase
import com.wandr.domain.usecase.GetMemberRankingUseCase
import com.wandr.domain.usecase.GetTeamStandingsUseCase
import com.wandr.domain.usecase.GetUserTeamsUseCase
import com.wandr.domain.usecase.JoinChallengeUseCase
import com.wandr.domain.usecase.LeaveChallengeUseCase
import com.wandr.domain.usecase.RefreshChallengesUseCase
import com.wandr.domain.usecase.RemoveChallengeCoverUseCase
import com.wandr.domain.usecase.SetChallengeCoverUseCase
import com.wandr.domain.usecase.UpdateChallengeUseCase
import com.wandr.domain.usecase.WithdrawTeamFromChallengeUseCase
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
    private val getChallengeParticipationsUseCase: GetChallengeParticipationsUseCase,
    private val leaveChallengeUseCase: LeaveChallengeUseCase,
    private val withdrawTeamFromChallengeUseCase: WithdrawTeamFromChallengeUseCase,
    private val getChallengeUseCase: GetChallengeUseCase,
    private val setChallengeCoverUseCase: SetChallengeCoverUseCase,
    private val removeChallengeCoverUseCase: RemoveChallengeCoverUseCase,
    private val evaluateChallengeStatusUseCase: EvaluateChallengeStatusUseCase,
    private val refreshChallengesUseCase: RefreshChallengesUseCase,
    private val getAdminTeamsUseCase: GetAdminTeamsUseCase,
    private val getUserTeamsUseCase: GetUserTeamsUseCase,
    private val createChallengeUseCase: CreateChallengeUseCase,
    private val updateChallengeUseCase: UpdateChallengeUseCase,
    private val getMemberRankingUseCase: GetMemberRankingUseCase,
    private val getTeamStandingsUseCase: GetTeamStandingsUseCase,
    private val enrollTeamInChallengeUseCase: EnrollTeamInChallengeUseCase,
    private val joinChallengeUseCase: JoinChallengeUseCase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val _uiState = MutableStateFlow(ChallengeState())
    val uiState: StateFlow<ChallengeState> = _uiState.asStateFlow()

    private var observeJob: Job? = null
    private var selectJob: Job? = null
    private var currentUserId: String? = null

    fun processIntent(intent: ChallengeIntent) {
        when (intent) {
            is ChallengeIntent.LoadChallenges -> load(intent.userId)
            is ChallengeIntent.SelectChallenge -> selectChallenge(intent.challengeId)
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
            is ChallengeIntent.ActivityTypeToggled -> updateForm { form ->
                form.copy(activityTypes = if (intent.type in form.activityTypes) form.activityTypes - intent.type else form.activityTypes + intent.type)
            }
            is ChallengeIntent.RequireAllMembersCompletionChanged -> updateForm { it.copy(requireAllMembersCompletion = intent.requireAll) }
            is ChallengeIntent.SubmitForm -> submit(intent.userId)
            is ChallengeIntent.DiscardForm -> _uiState.update { it.copy(form = null) }
            is ChallengeIntent.UploadCover -> updateCover { id -> setChallengeCoverUseCase(id, intent.jpegBytes) }
            is ChallengeIntent.RemoveCover -> updateCover { id -> removeChallengeCoverUseCase(id) }
            is ChallengeIntent.JoinChallenge -> joinChallenge(intent.challengeId, intent.userId)
            is ChallengeIntent.EnrollTeam -> enrollTeam(intent.challengeId, intent.teamId, intent.userId)
            is ChallengeIntent.LeaveChallenge -> leaveChallenge(intent.challengeId, intent.userId)
            is ChallengeIntent.WithdrawTeam -> withdrawTeam(intent.challengeId, intent.teamId)
            is ChallengeIntent.ClearMessages -> _uiState.update {
                it.copy(
                    error = null,
                    success = null
                )
            }
        }
    }

    private fun load(userId: String) {
        currentUserId = userId
        _uiState.update {
            it.copy(isLoading = it.challenges.isEmpty(), canEdit = it.selectedChallenge?.createdBy == userId)
        }
        observeJob?.cancel()
        observeJob = scope.launch {
            // The local cache drives the UI; the remote pull only feeds it (and may fail while offline).
            launch { refreshChallengesUseCase(userId) }
            launch {
                getChallengeParticipationsUseCase(userId).collect { list ->
                    _uiState.update { it.copy(participations = list.associateBy { p -> p.challengeId }) }
                }
            }
            launch {
                getAdminTeamsUseCase(userId).collect { teams ->
                    _uiState.update {
                        it.copy(
                            adminTeams = teams
                        )
                    }
                }
            }
            launch {
                // The first team by priority is the only one that counts for group challenges.
                getUserTeamsUseCase(userId).collect { teams ->
                    _uiState.update { it.copy(primaryTeamId = teams.firstOrNull()?.id) }
                }
            }
            getChallengesUseCase().collect { challenges ->
                val now = Clock.System.now().toEpochMilliseconds()
                val statuses = challenges.associate { it.id to evaluateChallengeStatusUseCase(it, currentTimeMillis = now) }
                _uiState.update { it.copy(challenges = challenges, statuses = statuses, isLoading = false) }
            }
        }
    }

    private fun selectChallenge(challengeId: String) {
        _uiState.update { it.copy(isLoading = true, standings = emptyList(), leaderboard = emptyList()) }
        selectJob?.cancel()
        selectJob = scope.launch {
            launch {
                getChallengeUseCase(challengeId).collect { challenge ->
                    _uiState.update {
                        it.copy(selectedChallenge = challenge, canEdit = challenge?.createdBy == currentUserId)
                    }
                }
            }
            loadRankings(challengeId)
        }
    }

    /**
     * Loads the standings and the member ranking from the server. The server calculates the progress, including the
     * activities from before joining / enrolling that lie within the challenge period.
     */
    private fun CoroutineScope.loadRankings(challengeId: String) {
        launch {
            // Team vs. team ranking (aggregates, from the server); may fail while offline.
            getTeamStandingsUseCase(challengeId)
                .onSuccess { standings ->
                    _uiState.update {
                        it.copy(
                            standings = standings,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.asAppError()
                        )
                    }
                }
        }
        launch {
            // The members the server lets this user see; fails quietly while offline (the list just stays empty).
            getMemberRankingUseCase(challengeId).onSuccess { ranking ->
                _uiState.update {
                    it.copy(
                        leaderboard = ranking
                    )
                }
            }
        }
    }

    /** Reloads the rankings after a participation changed, so the progress of past activities shows right away. */
    private fun reloadRankings(challengeId: String) {
        if (_uiState.value.selectedChallenge?.id == challengeId) scope.loadRankings(challengeId)
    }

    private fun startCreate() {
        // Whole minutes are enough for a start time; the end defaults to 30 days later.
        val now = Clock.System.now().toEpochMilliseconds().let { it - it % MINUTE_MILLIS }
        _uiState.update {
            it.copy(
                form = ChallengeForm(startDate = now, endDate = now + 30 * DAY_MILLIS),
                error = null
            )
        }
    }

    private fun startEdit(challengeId: String) {
        val state = _uiState.value
        val challenge = state.challenges.firstOrNull { it.id == challengeId }
            ?: state.selectedChallenge?.takeIf { it.id == challengeId }
            ?: return
        if (challenge.createdBy != currentUserId) {
            _uiState.update { it.copy(error = AppError.PermissionDenied()) }
            return
        }
        _uiState.update {
            it.copy(
                error = null,
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
                    activityTypes = challenge.activityTypes.toSet(),
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
            activityTypes = form.activityTypes.toList(),
            startDate = form.startDate,
            endDate = form.endDate,
            createdBy = existing?.createdBy ?: userId,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            isActive = form.isActive
        )

        _uiState.update { it.copy(isSaving = true, error = null, success = null) }
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
                    _uiState.update { it.copy(isSaving = false, error = error.asAppError()) }
                }
        }
    }

    private fun updateCover(action: suspend (challengeId: String) -> Result<Challenge>) {
        val challengeId = _uiState.value.selectedChallenge?.id ?: return
        if (!_uiState.value.canEdit) {
            _uiState.update { it.copy(error = AppError.PermissionDenied()) }
            return
        }
        _uiState.update { it.copy(isImageUpdating = true, error = null, success = null) }
        scope.launch {
            action(challengeId)
                .onSuccess { _uiState.update { it.copy(isImageUpdating = false, success = ChallengeSuccess.IMAGE_UPDATED) } }
                .onFailure { error ->
                    _uiState.update { it.copy(isImageUpdating = false, error = error.asAppError()) }
                }
        }
    }

    private fun defaultTarget(type: ChallengeType): Double = when (type) {
        ChallengeType.DISTANCE -> 100_000.0 // 100 km
        ChallengeType.ELEVATION -> 5_000.0 // 5000 m
        ChallengeType.TIME -> 36_000.0 // 10 h
    }

    private fun enrollTeam(challengeId: String, teamId: String, userId: String) {
        // Only the team's owner / admins may do this; the UI does not offer it otherwise, the server enforces it too.
        if (_uiState.value.adminTeams.none { it.id == teamId }) {
            _uiState.update { it.copy(error = AppError.PermissionDenied()) }
            return
        }
        scope.launch {
            enrollTeamInChallengeUseCase(challengeId, teamId, userId)
                .onSuccess {
                    _uiState.update { it.copy(success = ChallengeSuccess.TEAM_ENROLLED) }
                    // The server adds the team's members as participants; pull them so the state reflects it.
                    refreshChallengesUseCase(userId)
                    reloadRankings(challengeId)
                }
                .onFailure { error -> _uiState.update { it.copy(error = error.asAppError()) } }
        }
    }

    private fun leaveChallenge(challengeId: String, userId: String) {
        scope.launch {
            leaveChallengeUseCase(challengeId, userId)
                .onSuccess {
                    _uiState.update { it.copy(success = ChallengeSuccess.LEFT) }
                    reloadRankings(challengeId)
                }
                .onFailure { error -> _uiState.update { it.copy(error = error.asAppError()) } }
        }
    }

    private fun withdrawTeam(challengeId: String, teamId: String) {
        if (_uiState.value.adminTeams.none { it.id == teamId }) {
            _uiState.update { it.copy(error = AppError.PermissionDenied()) }
            return
        }
        scope.launch {
            withdrawTeamFromChallengeUseCase(challengeId, teamId)
                .onSuccess {
                    _uiState.update { it.copy(success = ChallengeSuccess.TEAM_WITHDRAWN) }
                    reloadRankings(challengeId)
                }
                .onFailure { error -> _uiState.update { it.copy(error = error.asAppError()) } }
        }
    }

    private fun joinChallenge(challengeId: String, userId: String) {
        scope.launch {
            joinChallengeUseCase(challengeId, userId)
                .onSuccess {
                    _uiState.update { it.copy(success = ChallengeSuccess.JOINED) }
                    reloadRankings(challengeId)
                }
                .onFailure { error -> _uiState.update { it.copy(error = error.asAppError()) } }
        }
    }

    private companion object {
        const val MINUTE_MILLIS = 60_000L
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
