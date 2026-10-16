import SwiftUI
@preconcurrency import shared

/// Stateless list of notifications, grouped by day into connected cards (newest first, as loaded).
struct NotificationsListView: View {
    let notifications: [AppNotification]
    let isLoading: Bool
    var onSelect: (AppNotification) -> Void = { _ in }

    private struct DayGroup: Identifiable {
        let id: Date
        let items: [AppNotification]
    }

    private var groups: [DayGroup] {
        var result: [DayGroup] = []
        for item in notifications {
            let day = Calendar.current.startOfDay(for: Date(timeIntervalSince1970: Double(item.createdAt) / 1000))
            if let last = result.last, last.id == day {
                result[result.count - 1] = DayGroup(id: day, items: last.items + [item])
            } else {
                result.append(DayGroup(id: day, items: [item]))
            }
        }
        return result
    }

    /// "Today", "Yesterday" or the date, localized by the system.
    private static let dayFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateStyle = .full
        formatter.timeStyle = .none
        formatter.doesRelativeDateFormatting = true
        return formatter
    }()

    var body: some View {
        ScrollView {
            if notifications.isEmpty {
                Group {
                    if isLoading {
                        ProgressView()
                    } else {
                        ContentUnavailableView {
                            Label(LocalizedStringKey("notifications_empty"), systemImage: "bell.slash")
                        }
                    }
                }
                .frame(maxWidth: .infinity)
                .padding(.top, 80)
            } else {
                LazyVStack(alignment: .leading, spacing: 2) {
                    ForEach(groups) { group in
                        Text(Self.dayFormatter.string(from: group.id))
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(.secondary)
                            .padding(.horizontal, 8)
                            .padding(.top, group.id == groups.first?.id ? 8 : 20)
                            .padding(.bottom, 6)
                            .accessibilityAddTraits(.isHeader)
                        ForEach(Array(group.items.enumerated()), id: \.element.id) { index, item in
                            Button { onSelect(item) } label: {
                                NotificationRowView(notification: item, isFirst: index == 0, isLast: index == group.items.count - 1)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 24)
            }
        }
        .background(Color(.systemGroupedBackground))
    }
}

#Preview("Light Mode") {
    NotificationsListView(
        notifications: [.sample(), .sample(id: "n2", type: .like, isRead: true), .sample(id: "n3", type: .reaction, entityType: .challenge)],
        isLoading: false
    )
    .preferredColorScheme(.light)
}

#Preview("Dark Mode, empty") {
    NotificationsListView(notifications: [], isLoading: false).preferredColorScheme(.dark)
}

#Preview("Large Text") {
    NotificationsListView(notifications: [.sample()], isLoading: false).environment(\.dynamicTypeSize, .accessibility2)
}
