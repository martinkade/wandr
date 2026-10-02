import SwiftUI
@preconcurrency import shared

@MainActor
final class NotificationsObserver: ObservableObject {
    @Published var notifications: [AppNotification] = []
    @Published var unreadCount = 0
    @Published var isLoading = false

    private let viewModel = IosDependencies.shared.notificationsViewModel()
    nonisolated(unsafe) private var job: (any Kotlinx_coroutines_coreJob)?
    private var userId: String?

    func start(userId: String) {
        guard self.userId != userId else { return }
        self.userId = userId
        job?.cancel(cause: nil)
        job = FlowObserverKt.watch(viewModel.state) { [weak self] value in
            guard let state = value as? NotificationsState else { return }
            Task { @MainActor in
                self?.notifications = state.notifications
                self?.unreadCount = Int(state.unreadCount)
                self?.isLoading = state.isLoading
            }
        }
        viewModel.processIntent(intent: NotificationsIntentRefreshBadge(userId: userId))
    }

    func load() {
        guard let userId else { return }
        viewModel.processIntent(intent: NotificationsIntentLoad(userId: userId))
    }

    func refreshBadge() {
        guard let userId else { return }
        viewModel.processIntent(intent: NotificationsIntentRefreshBadge(userId: userId))
    }

    func markRead(_ id: String) { viewModel.processIntent(intent: NotificationsIntentMarkRead(id: id)) }
    func markAllRead() { viewModel.processIntent(intent: NotificationsIntentMarkAllRead.shared) }

    deinit { job?.cancel(cause: nil) }
}
