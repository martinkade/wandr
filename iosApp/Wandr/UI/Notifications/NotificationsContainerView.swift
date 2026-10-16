import SwiftUI
@preconcurrency import shared

/// Where a tapped notification leads inside the notifications sheet.
enum NotificationDestination: Hashable {
    case activity(String)
    case challenge(String)
}

/// The notifications sheet: loads the list, marks a tapped entry as read and pushes its activity or challenge on the
/// sheet's own stack, so "back" returns to the list (like Android's overlay above the list). The challenge details
/// come from the caller ([challengeDetail]); pass the bell's [observer] so the badge stays in sync.
struct NotificationsContainerView<ChallengeDetail: View>: View {
    let userId: String
    @ObservedObject var observer: NotificationsObserver
    var onClose: () -> Void
    let challengeDetail: (String) -> ChallengeDetail

    @State private var path: [NotificationDestination] = []
    @State private var message: SnackbarMessage?

    init(
        userId: String, observer: NotificationsObserver, onClose: @escaping () -> Void,
        @ViewBuilder challengeDetail: @escaping (String) -> ChallengeDetail
    ) {
        self.userId = userId
        self.observer = observer
        self.onClose = onClose
        self.challengeDetail = challengeDetail
    }

    var body: some View {
        NavigationStack(path: $path) {
            NotificationsListView(notifications: observer.notifications, isLoading: observer.isLoading) { item in
                observer.markRead(item.id)
                path.append(item.entityType == .challenge ? .challenge(item.entityId) : .activity(item.entityId))
            }
            .refreshable { observer.load() }
            .navigationTitle(LocalizedStringKey("notifications_title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                NotificationsToolbar(hasUnread: observer.unreadCount > 0, onMarkAllRead: observer.markAllRead, onClose: onClose)
            }
            .navigationDestination(for: NotificationDestination.self) { destination in
                switch destination {
                case .activity(let id): ActivityDetailView(activityId: id, userId: userId)
                case .challenge(let id): challengeDetail(id)
                }
            }
        }
        .snackbar($message)
        .task(id: userId) {
            observer.start(userId: userId)
            observer.load()
        }
        .onChange(of: observer.errorMessage) { _, error in
            guard let error else { return }
            message = SnackbarMessage(text: error, isError: true)
            observer.clearError()
        }
    }
}

// The container needs the shared view models (Koin); see `NotificationsListView` for the content previews.
#Preview("Light Mode") {
    NotificationsContainerView(userId: "u1", observer: NotificationsObserver(), onClose: {}) { Text($0) }.preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    NotificationsContainerView(userId: "u1", observer: NotificationsObserver(), onClose: {}) { Text($0) }.preferredColorScheme(.dark)
}

#Preview("Large Text") {
    NotificationsContainerView(userId: "u1", observer: NotificationsObserver(), onClose: {}) { Text($0) }
        .environment(\.dynamicTypeSize, .accessibility2)
}
