import SwiftUI
@preconcurrency import shared

/// The shared settings view model for SwiftUI.
@MainActor
final class SettingsObserver: ObservableObject {
    @Published private(set) var pushEnabled = true
    @Published private(set) var isUpdatingPush = false
    /// What to be notified about; nil until loaded from the server.
    @Published private(set) var preferences: NotificationPreferences?
    @Published private(set) var isResettingAccount = false
    @Published private(set) var isDeletingAccount = false
    @Published private(set) var success: SettingsSuccess?
    @Published var errorMessage: String?

    private let viewModel = IosDependencies.shared.settingsViewModel()
    nonisolated(unsafe) private var job: (any Kotlinx_coroutines_coreJob)?

    var isBusyWithAccount: Bool {
        isResettingAccount || isDeletingAccount
    }

    func start(userId: String) {
        job?.cancel(cause: nil)
        job = FlowObserverKt.watch(viewModel.state) { [weak self] value in
            guard let state = value as? SettingsState else { return }
            Task { @MainActor in
                self?.pushEnabled = state.pushEnabled
                self?.isUpdatingPush = state.isUpdatingPush
                self?.preferences = state.notificationPreferences
                self?.isResettingAccount = state.isResettingAccount
                self?.isDeletingAccount = state.isDeletingAccount
                self?.success = state.success
                self?.errorMessage = state.error?.userMessage
            }
        }
        viewModel.processIntent(intent: SettingsIntentLoad(userId: userId))
    }

    func setPushEnabled(_ enabled: Bool) {
        viewModel.processIntent(intent: SettingsIntentSetPushEnabled(enabled: enabled))
    }

    func setScope(_ scope: NotificationScope, enabled: Bool) {
        viewModel.processIntent(intent: SettingsIntentSetNotificationScope(scope: scope, enabled: enabled))
    }

    func resetAccount() {
        viewModel.processIntent(intent: SettingsIntentResetAccount.shared)
    }

    func deleteAccount() {
        viewModel.processIntent(intent: SettingsIntentDeleteAccount.shared)
    }

    func clearMessages() {
        viewModel.processIntent(intent: SettingsIntentClearMessages.shared)
    }

    deinit { job?.cancel(cause: nil) }
}
