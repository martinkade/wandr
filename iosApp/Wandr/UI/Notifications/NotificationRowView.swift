import SwiftUI
@preconcurrency import shared

struct NotificationRowView: View {
    let notification: AppNotification

    private var isChallenge: Bool { notification.entityType == .challenge }

    private var text: String {
        let actor = notification.actorName
        switch notification.type {
        case .like:
            return String(format: NSLocalizedString(isChallenge ? "notification_like_challenge" : "notification_like_activity", comment: ""), actor)
        case .comment:
            return String(format: NSLocalizedString(isChallenge ? "notification_comment_challenge" : "notification_comment_activity", comment: ""), actor)
        case .reaction:
            return String(format: NSLocalizedString("notification_reaction", comment: ""), actor, notification.emoji ?? "")
        case .invite:
            return String(format: NSLocalizedString("notification_invite", comment: ""), actor)
        default:
            return String(format: NSLocalizedString("notification_unknown", comment: ""), actor)
        }
    }

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Circle()
                .fill(notification.isRead ? Color.clear : Color.wandrAccentText)
                .frame(width: 8, height: 8)
                .padding(.top, 6)
            VStack(alignment: .leading, spacing: 4) {
                Text(text).font(.subheadline).fontWeight(notification.isRead ? .regular : .semibold)
                if let preview = notification.preview, !preview.isEmpty {
                    Text(preview).font(.footnote).foregroundColor(.secondary).lineLimit(2)
                }
                Text(Date(timeIntervalSince1970: Double(notification.createdAt) / 1000), style: .relative)
                    .font(.caption).foregroundColor(.secondary)
            }
        }
    }
}

extension AppNotification {
    static func sample(type: NotificationType = .comment, isRead: Bool = false) -> AppNotification {
        AppNotification(
            id: "n1", type: type, actorUserId: "u2", actorName: "Alex", actorAvatarUrl: nil,
            entityType: .activity, entityId: "a1", commentId: "c1", preview: "Great hike!", emoji: "🔥",
            isRead: isRead, createdAt: 1_700_000_000_000
        )
    }
}

#Preview("Light Mode") {
    List { NotificationRowView(notification: .sample()); NotificationRowView(notification: .sample(type: .reaction, isRead: true)) }
        .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    List { NotificationRowView(notification: .sample()); NotificationRowView(notification: .sample(type: .like, isRead: true)) }
        .preferredColorScheme(.dark)
}
