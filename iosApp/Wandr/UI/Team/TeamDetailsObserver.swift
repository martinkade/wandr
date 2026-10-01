@preconcurrency import shared
import SwiftUI

/// Bridges the shared TeamDetailsViewModel into SwiftUI.
@MainActor
final class TeamDetailsObserver: ObservableObject {
    @Published var name = ""
    @Published var teamDescription = ""
    @Published var avatarUrl: String?
    @Published var coverUrl: String?
    @Published var inviteCode = ""
    @Published var members: [TeamMember] = []
    @Published var canEdit = false
    @Published var isLoading = true
    @Published var isSaving = false
    @Published var isImageUpdating = false
    @Published var snackbar: SnackbarMessage?
    /// Increments after each successful save so the view can leave edit mode.
    @Published var saveCompletedCount = 0

    private let viewModel = IosDependencies.shared.teamDetailsViewModel()
    nonisolated(unsafe) private var job: (any Kotlinx_coroutines_coreJob)?
    private var loadedTeamId: String?

    func load(teamId: String, userId: String) {
        guard loadedTeamId != teamId else { return }
        loadedTeamId = teamId
        job?.cancel(cause: nil)
        job = FlowObserverKt.watch(viewModel.uiState) { [weak self] value in
            guard let state = value as? TeamDetailsState else { return }
            Task { @MainActor in self?.apply(state) }
        }
        viewModel.processIntent(intent: TeamDetailsIntentLoad(teamId: teamId, userId: userId))
    }

    func setName(_ value: String) {
        name = value
        viewModel.processIntent(intent: TeamDetailsIntentNameChanged(name: value))
    }

    func setDescription(_ value: String) {
        teamDescription = value
        viewModel.processIntent(intent: TeamDetailsIntentDescriptionChanged(description: value))
    }

    func save() { viewModel.processIntent(intent: TeamDetailsIntentSave.shared) }

    /// The next state (no unsaved changes) restores name / description from the stored team.
    func discard() { viewModel.processIntent(intent: TeamDetailsIntentDiscardChanges.shared) }

    func uploadAvatar(_ jpeg: Data) {
        viewModel.processIntent(intent: TeamDetailsIntentUploadAvatar(jpegBytes: KotlinByteArray.from(data: jpeg)))
    }

    func uploadCover(_ jpeg: Data) {
        viewModel.processIntent(intent: TeamDetailsIntentUploadCover(jpegBytes: KotlinByteArray.from(data: jpeg)))
    }

    func removeAvatar() { viewModel.processIntent(intent: TeamDetailsIntentRemoveAvatar.shared) }
    func removeCover() { viewModel.processIntent(intent: TeamDetailsIntentRemoveCover.shared) }

    private func apply(_ state: TeamDetailsState) {
        avatarUrl = state.team?.avatarUrl
        coverUrl = state.team?.coverUrl
        inviteCode = state.team?.inviteCode ?? ""
        members = state.members
        canEdit = state.canEdit
        isLoading = state.isLoading
        isSaving = state.isSaving
        isImageUpdating = state.isImageUpdating
        // While the user is editing, the local text fields are the source of truth.
        if !state.hasUnsavedChanges, let team = state.team {
            name = team.name
            teamDescription = team.description_ ?? ""
        }
        if let error = state.errorMessage {
            snackbar = SnackbarMessage(text: error, isError: true)
            viewModel.processIntent(intent: TeamDetailsIntentClearMessages.shared)
        } else if let success = state.success {
            switch success {
            case .teamSaved:
                snackbar = SnackbarMessage(text: String(localized: "team_saved_message"))
                saveCompletedCount += 1
            case .imageUpdated:
                snackbar = SnackbarMessage(text: String(localized: "team_image_updated_message"))
            default: break
            }
            viewModel.processIntent(intent: TeamDetailsIntentClearMessages.shared)
        }
    }

    deinit { job?.cancel(cause: nil) }
}
