import SwiftUI
@preconcurrency import shared

/// The shared settings view model for SwiftUI.
@MainActor
final class SettingsObserver: ObservableObject {
    @Published private(set) var pushEnabled = true
    @Published private(set) var isUpdatingPush = false
    @Published var errorMessage: String?

    private let viewModel = IosDependencies.shared.settingsViewModel()
    nonisolated(unsafe) private var job: (any Kotlinx_coroutines_coreJob)?

    func start() {
        job?.cancel(cause: nil)
        job = FlowObserverKt.watch(viewModel.state) { [weak self] value in
            guard let state = value as? SettingsState else { return }
            Task { @MainActor in
                self?.pushEnabled = state.pushEnabled
                self?.isUpdatingPush = state.isUpdatingPush
                self?.errorMessage = state.error?.userMessage
            }
        }
        viewModel.processIntent(intent: SettingsIntentLoad.shared)
    }

    func setPushEnabled(_ enabled: Bool) {
        viewModel.processIntent(intent: SettingsIntentSetPushEnabled(enabled: enabled))
    }

    func clearError() {
        viewModel.processIntent(intent: SettingsIntentClearMessages.shared)
    }

    deinit { job?.cancel(cause: nil) }
}
