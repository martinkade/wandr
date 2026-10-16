import SwiftUI

/// Close and "mark all read" of the notifications sheet.
struct NotificationsToolbar: ToolbarContent {
    let hasUnread: Bool
    var onMarkAllRead: () -> Void
    var onClose: () -> Void

    var body: some ToolbarContent {
        ToolbarItem(placement: .cancellationAction) {
            Button(action: onClose) { Image(systemName: "xmark") }
                .accessibilityLabel(Text(LocalizedStringKey("notifications_close")))
        }
        ToolbarItem(placement: .primaryAction) {
            Button(action: onMarkAllRead) { Label(LocalizedStringKey("notifications_mark_all_read"), systemImage: "checkmark.circle") }
                .labelStyle(.titleOnly)
                .disabled(!hasUnread)
        }
    }
}

#Preview("Light Mode") {
    NavigationStack {
        Text("Notifications").toolbar { NotificationsToolbar(hasUnread: true, onMarkAllRead: {}, onClose: {}) }
    }
    .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    NavigationStack {
        Text("Notifications").toolbar { NotificationsToolbar(hasUnread: false, onMarkAllRead: {}, onClose: {}) }
    }
    .preferredColorScheme(.dark)
}

#Preview("Large Text") {
    NavigationStack {
        Text("Notifications").toolbar { NotificationsToolbar(hasUnread: true, onMarkAllRead: {}, onClose: {}) }
    }
    .environment(\.dynamicTypeSize, .accessibility2)
}
