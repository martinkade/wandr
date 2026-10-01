@preconcurrency import shared
import SwiftUI

/// Bridges the shared TeamViewModel into SwiftUI.
@MainActor
final class TeamObserver: ObservableObject {
    @Published var teams: [Team] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    /// Increments after each successful creation so the view can dismiss its sheet.
    @Published var createdCount = 0

    private let viewModel = IosDependencies.shared.teamViewModel()
    nonisolated(unsafe) private var job: (any Kotlinx_coroutines_coreJob)?
    private var userId: String?
    private var awaitingCreate = false

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
        awaitingCreate = true
        errorMessage = nil
        viewModel.processIntent(intent: TeamIntentClearMessages.shared)
        viewModel.processIntent(intent: TeamIntentCreateTeamNameChanged(name: name))
        viewModel.processIntent(intent: TeamIntentCreateTeamDescriptionChanged(description: description))
        viewModel.processIntent(intent: TeamIntentSubmitCreateTeam(creatorId: userId))
    }

    func resetMessages() {
        errorMessage = nil
        viewModel.processIntent(intent: TeamIntentClearMessages.shared)
    }

    private func apply(_ state: TeamState) {
        teams = state.teams
        isLoading = state.isLoading
        errorMessage = state.errorMessage
        if awaitingCreate, !state.isLoading {
            if state.successMessage != nil {
                awaitingCreate = false
                createdCount += 1
                viewModel.processIntent(intent: TeamIntentClearMessages.shared)
            } else if state.errorMessage != nil {
                awaitingCreate = false
            }
        }
    }

    deinit { job?.cancel(cause: nil) }
}
