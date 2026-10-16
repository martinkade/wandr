import SwiftUI
@preconcurrency import shared

/// Whose activities the feed shows.
enum FeedScope: String, CaseIterable, Identifiable {
    case mine, team
    var id: String { rawValue }
    var title: LocalizedStringKey { self == .mine ? "activity_feed_mine" : "activity_feed_team" }
}

/// Stateless feed content: the Mine / Team switch (Team only with a team), the activity cards, loading and empty state.
/// `FeedContainerView` feeds it from the view model.
struct FeedView: View {
    let activities: [Activity]
    var authors: [String: Profile] = [:]
    var socialCounts: [String: SocialCounts] = [:]
    var userId: String?
    var isLoading = false
    var scope: Binding<FeedScope> = .constant(.mine)
    var showsTeamScope = false
    var onSelectActivity: (Activity) -> Void = { _ in }
    var onToggleLike: (String) -> Void = { _ in }

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                if showsTeamScope {
                    Picker(selection: scope) {
                        ForEach(FeedScope.allCases) { Text($0.title).tag($0) }
                    } label: { EmptyView() }
                    .pickerStyle(.segmented)
                }

                if activities.isEmpty {
                    emptyState
                } else {
                    ForEach(activities, id: \.id) { activity in
                        let author = authors[activity.userId]
                        let counts = socialCounts[activity.id]
                        ActivityCardView(
                            activity: activity,
                            authorName: author?.displayName ?? "",
                            authorAvatarUrl: author?.avatarUrl,
                            likeCount: Int(counts?.likeCount ?? 0),
                            likedByMe: counts?.likedByMe ?? false,
                            commentCount: Int(counts?.commentCount ?? 0),
                            isOwn: activity.userId == userId,
                            onOpen: { onSelectActivity(activity) },
                            onToggleLike: { onToggleLike(activity.id) }
                        )
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
            // Keeps the last card clear of the floating button.
            .padding(.bottom, 96)
        }
        .background(Color(.systemGroupedBackground))
    }

    @ViewBuilder
    private var emptyState: some View {
        if isLoading {
            ProgressView()
                .frame(maxWidth: .infinity)
                .padding(.top, 80)
        } else {
            ContentUnavailableView {
                Label(LocalizedStringKey("activities_empty"), systemImage: "figure.hiking")
            }
            .padding(.top, 40)
        }
    }
}

#Preview("Light Mode") {
    FeedView(
        activities: [.sample(), .sample(id: "a2", userId: "u2", title: "Morning ride", type: "cycling", polyline: nil)],
        socialCounts: ["a1": SocialCounts(likeCount: 5, commentCount: 2, likedByMe: true)],
        userId: "u1", showsTeamScope: true
    )
    .preferredColorScheme(.light)
}

#Preview("Dark Mode, empty") {
    FeedView(activities: [], showsTeamScope: true).preferredColorScheme(.dark)
}

#Preview("Large Text") {
    FeedView(activities: [.sample()], userId: "u1").environment(\.dynamicTypeSize, .accessibility2)
}
