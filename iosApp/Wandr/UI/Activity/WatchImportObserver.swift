import SwiftUI
@preconcurrency import shared

/// Imports workouts that arrive from the watch while the main screen is shown, and publishes a conflict for the wizard.
@MainActor
final class WatchImportObserver: ObservableObject {
    @Published var conflict: ActivityConflict?
    @Published var isSaving = false
    @Published var importedCount = 0

    private let viewModel = IosDependencies.shared.watchImportViewModel()
    nonisolated(unsafe) private var job: (any Kotlinx_coroutines_coreJob)?

    func start(userId: String, teamId: String?) {
        job?.cancel(cause: nil)
        job = FlowObserverKt.watch(viewModel.state) { [weak self] value in
            guard let state = value as? WatchImportState else { return }
            Task { @MainActor in
                self?.conflict = state.conflict
                self?.isSaving = state.isSaving
                self?.importedCount = Int(state.importedCount)
            }
        }
        viewModel.processIntent(intent: WatchImportIntentStart(userId: userId, teamId: teamId))
    }

    func resolve(_ resolution: ConflictResolution) {
        viewModel.processIntent(intent: WatchImportIntentResolveConflict(resolution: resolution))
    }

    func clearMessages() {
        viewModel.processIntent(intent: WatchImportIntentClearMessages.shared)
    }

    deinit { job?.cancel(cause: nil) }
}
