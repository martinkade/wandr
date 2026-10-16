@preconcurrency import shared
import SwiftUI

/// Bridges the shared TeamViewModel into SwiftUI: the user's teams (priority order), creating, joining and reordering.
@MainActor
final class TeamObserver: ObservableObject {
    @Published var teams: [Team] = []
    @Published var isLoading = false
    @Published var isCreating = false
    @Published var isJoining = false
    @Published var joinCode = ""
    /// Shown inside the join sheet (e.g. an invalid code).
    @Published var joinErrorMessage: String?
    /// Results outside a sheet (joined, a rejected reorder) and create errors.
    @Published var snackbar: SnackbarMessage?
    /// Increment after each success so views can dismiss their sheets.
    @Published var createdCount = 0
    @Published var joinedCount = 0

    /// The view model shares `isLoading` / messages between actions; this tells whose result a state carries.
    private enum Pending { case none, create, join }

    private let viewModel = IosDependencies.shared.teamViewModel()
    nonisolated(unsafe) private var job: (any Kotlinx_coroutines_coreJob)?
    private var userId: String?
    private var pending = Pending.none

    func load(userId: String) {
        guard self.userId != userId else { return }
        self.userId = userId
        job?.cancel(cause: nil)
        job = FlowObserverKt.watch(viewModel.uiState) { [weak self] value in
            guard let state = value as? TeamState else { return }
            Task { @MainActor in self?.apply(state) }
        }
        viewModel.processIntent(intent: TeamIntentLoadUserTeams(userId: userId))
    }

    func create(name: String, description: String) {
        guard let userId else { return }
        pending = .create
        isCreating = true
        viewModel.processIntent(intent: TeamIntentClearMessages.shared)
        viewModel.processIntent(intent: TeamIntentCreateTeamNameChanged(name: name))
        viewModel.processIntent(intent: TeamIntentCreateTeamDescriptionChanged(description: description))
        viewModel.processIntent(intent: TeamIntentSubmitCreateTeam(creatorId: userId))
    }

    func setJoinCode(_ code: String) {
        joinCode = code
        joinErrorMessage = nil
        viewModel.processIntent(intent: TeamIntentJoinInviteCodeChanged(code: code))
    }

    /// `code` may be the bare invite code or a scanned `wandr://invite/{code}`; the shared use case unwraps it.
    func join(code: String? = nil) {
        guard let userId else { return }
        if let code { setJoinCode(code) }
        pending = .join
        isJoining = true
        joinErrorMessage = nil
        viewModel.processIntent(intent: TeamIntentClearMessages.shared)
        viewModel.processIntent(intent: TeamIntentSubmitJoinTeam(userId: userId))
    }

    func resetJoin() {
        setJoinCode("")
        joinErrorMessage = nil
    }

    /// `teamIds` is the new priority order, highest first. The view model shows it at once and reverts on failure.
    func reorder(teamIds: [String]) {
        guard let userId else { return }
        viewModel.processIntent(intent: TeamIntentReorderTeams(userId: userId, teamIds: teamIds))
    }

    private func apply(_ state: TeamState) {
        teams = state.teams
        isLoading = state.isLoading
        let error = state.error?.userMessage

        switch pending {
        case .create:
            isCreating = state.isLoading
            guard !state.isLoading else { return }
            if state.successMessage != nil {
                createdCount += 1
            } else if let error {
                snackbar = SnackbarMessage(text: error, isError: true)
            } else { return }
            pending = .none
            viewModel.processIntent(intent: TeamIntentClearMessages.shared)
        case .join:
            isJoining = state.isLoading
            guard !state.isLoading else { return }
            if state.successMessage != nil {
                joinedCount += 1
                joinCode = ""
                snackbar = SnackbarMessage(text: String(localized: "join_team_success"))
            } else if let error {
                joinErrorMessage = error
            } else { return }
            pending = .none
            viewModel.processIntent(intent: TeamIntentClearMessages.shared)
        case .none:
            // E.g. a reorder the server rejected (already reverted by the view model).
            if let error {
                snackbar = SnackbarMessage(text: error, isError: true)
                viewModel.processIntent(intent: TeamIntentClearMessages.shared)
            }
        }
    }

    deinit { job?.cancel(cause: nil) }
}
