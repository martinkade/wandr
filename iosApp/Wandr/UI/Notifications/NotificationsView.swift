import SwiftUI
@preconcurrency import shared

/// Stateless list of notifications; the sheet content of the bell button.
struct NotificationsView: View {
    let notifications: [AppNotification]
    let isLoading: Bool
    var onSelect: (AppNotification) -> Void = { _ in }
    var onMarkAllRead: () -> Void = {}
    var onClose: () -> Void = {}

    var body: some View {
        NavigationStack {
            Group {
                if notifications.isEmpty {
                    Text(LocalizedStringKey(isLoading ? "notifications_loading" : "notifications_empty"))
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    List(notifications, id: \.id) { item in
                        Button { onSelect(item) } label: { NotificationRowView(notification: item) }
                            .buttonStyle(.plain)
                    }
                }
            }
            .navigationTitle(LocalizedStringKey("notifications_title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(LocalizedStringKey("notifications_close"), action: onClose)
                }
                ToolbarItem(placement: .primaryAction) {
                    Button(LocalizedStringKey("notifications_mark_all_read"), action: onMarkAllRead)
                        .disabled(notifications.allSatisfy { $0.isRead })
                }
            }
        }
    }
}

#Preview("Light Mode") {
    NotificationsView(notifications: [.sample(), .sample(type: .reaction, isRead: true)], isLoading: false)
        .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    NotificationsView(notifications: [], isLoading: false).preferredColorScheme(.dark)
}
