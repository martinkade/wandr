import SwiftUI
@preconcurrency import shared

/// One notification as a card of a grouped list: the actor's avatar, what happened, the time; unread ones are bold
/// with a dot. [isFirst] / [isLast] round the corners of the group's ends.
struct NotificationRowView: View {
    let notification: AppNotification
    var isFirst = true
    var isLast = true

    private var isChallenge: Bool { notification.entityType == .challenge }
    private var actor: String { notification.actorName.isEmpty ? String(localized: "social_unknown_author") : notification.actorName }

    private var text: String {
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

    private var symbol: String {
        switch notification.type {
        case .like: return "heart.fill"
        case .comment: return "bubble.left.fill"
        case .reaction: return "face.smiling.inverse"
        default: return "bell.fill"
        }
    }

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            AvatarView(url: notification.actorAvatarUrl, name: actor, size: 44)
                .overlay(alignment: .bottomTrailing) {
                    Image(systemName: symbol)
                        .font(.system(size: 10, weight: .bold))
                        .foregroundStyle(Color.wandrOnPrimary)
                        .frame(width: 20, height: 20)
                        .background(Color.wandrPrimary, in: Circle())
                        .overlay(Circle().stroke(Color(.secondarySystemGroupedBackground), lineWidth: 2))
                        .offset(x: 4, y: 4)
                }
            VStack(alignment: .leading, spacing: 4) {
                Text(text)
                    .font(.subheadline)
                    .fontWeight(notification.isRead ? .regular : .semibold)
                    .foregroundStyle(.primary)
                    .multilineTextAlignment(.leading)
                if let preview = notification.preview, !preview.isEmpty {
                    Text("“\(preview)”")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)
                }
                Text(Date(timeIntervalSince1970: Double(notification.createdAt) / 1000), style: .time)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer(minLength: 0)
            if !notification.isRead {
                Circle()
                    .fill(Color.wandrAccentText)
                    .frame(width: 10, height: 10)
                    .padding(.top, 6)
                    .accessibilityHidden(true)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .wandrSectionItem(isFirst: isFirst, isLast: isLast)
        .contentShape(Rectangle())
        .accessibilityElement(children: .combine)
    }
}

extension AppNotification {
    static func sample(id: String = "n1", type: NotificationType = .comment, entityType: SocialEntityType = .activity, isRead: Bool = false) -> AppNotification {
        AppNotification(
            id: id, type: type, actorUserId: "u2", actorName: "Alex Kim", actorAvatarUrl: nil,
            entityType: entityType, entityId: "a1", commentId: "c1", preview: type == .comment ? "Great hike!" : nil, emoji: "🔥",
            isRead: isRead, createdAt: 1_768_435_200_000
        )
    }
}

#Preview("Light Mode") {
    VStack(spacing: 2) {
        NotificationRowView(notification: .sample(), isFirst: true, isLast: false)
        NotificationRowView(notification: .sample(id: "n2", type: .reaction, isRead: true), isFirst: false, isLast: false)
        NotificationRowView(notification: .sample(id: "n3", type: .like, entityType: .challenge), isFirst: false, isLast: true)
    }
    .padding()
    .background(Color(.systemGroupedBackground))
    .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    NotificationRowView(notification: .sample(type: .like))
        .padding()
        .background(Color(.systemGroupedBackground))
        .preferredColorScheme(.dark)
}

#Preview("Large Text") {
    NotificationRowView(notification: .sample())
        .padding()
        .background(Color(.systemGroupedBackground))
        .environment(\.dynamicTypeSize, .accessibility2)
}
