import SwiftUI
@preconcurrency import shared

/// The feed tab's content: own view model, Mine / Team switch, cards with likes, pull to refresh, the floating
/// record / log-manually button and the create form. It lives inside the tab's NavigationStack; opening an activity is
/// reported via [onOpenActivity] so the same destination also serves push notifications.
struct FeedContainerView: View {
    let userId: String?
    let teamId: String?
    var isRecording = false
    var onOpenActivity: (String) -> Void = { _ in }
    var onRecord: () -> Void = {}
    /// Extra work on pull to refresh, e.g. the Apple Health import.
    var onRefresh: (() async -> Void)?

    @StateObject private var observer = ActivityObserver()
    @State private var scope: FeedScope = .mine
    @State private var isCreating = false
    @State private var message: SnackbarMessage?

    private var state: ActivityState { observer.state }
    /// The team only while the team feed is shown: a team arriving after the start must not reload "mine".
    private var teamFeedId: String? { scope == .team ? teamId : nil }

    var body: some View {
        Group {
            if let userId {
                FeedView(
                    activities: state.activities,
                    authors: state.authors,
                    socialCounts: state.socialCounts,
                    userId: userId,
                    isLoading: state.isLoading,
                    scope: $scope,
                    showsTeamScope: teamId != nil,
                    onSelectActivity: { onOpenActivity($0.id) },
                    onToggleLike: { observer.send(ActivityIntentToggleActivityLike(activityId: $0)) }
                )
                .refreshable {
                    load(userId)
                    await onRefresh?()
                    // The reload has no completion signal; keep the spinner for a moment so it does not just flash.
                    try? await Task.sleep(for: .milliseconds(600))
                }
                .overlay(alignment: .bottomTrailing) {
                    ActivityFabMenu(
                        isRecording: isRecording,
                        onRecord: onRecord,
                        onCreate: {
                            observer.send(ActivityIntentStartCreate.shared)
                            isCreating = true
                        }
                    )
                    .padding(20)
                }
                .task(id: "\(userId)|\(teamFeedId ?? "")") { load(userId) }
                .activityFormSheet(observer: observer, isPresented: $isCreating, message: $message, userId: userId, teamId: teamId)
            } else {
                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .background(Color(.systemGroupedBackground))
        .snackbar($message)
        .onChange(of: teamId) { _, newValue in if newValue == nil { scope = .mine } }
    }

    private func load(_ userId: String) {
        if let teamFeedId {
            observer.send(ActivityIntentLoadTeamActivities(teamId: teamFeedId, userId: userId))
        } else {
            observer.send(ActivityIntentLoadUserActivities(userId: userId))
        }
    }
}

// The container needs the shared view models (Koin); without a user it only shows the loading state.
#Preview("Light Mode") {
    NavigationStack { FeedContainerView(userId: nil, teamId: nil).navigationTitle("Feed") }.preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    NavigationStack { FeedContainerView(userId: nil, teamId: nil).navigationTitle("Feed") }.preferredColorScheme(.dark)
}

#Preview("Large Text") {
    FeedContainerView(userId: nil, teamId: nil).environment(\.dynamicTypeSize, .accessibility2)
}
