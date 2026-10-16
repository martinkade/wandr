import SwiftUI
@preconcurrency import shared

/// The challenges the user can see as cards, built from plain values (the observer lives in `ChallengesContainerView`).
/// The caller filters drafts; joining, leaving and editing happen on the details.
struct ChallengeListView: View {
    var challenges: [Challenge] = []
    var statuses: [String: ChallengeStatus] = [:]
    var socialCounts: [String: SocialCounts] = [:]
    var isLoading: Bool = false
    /// The card's zoom transition source (iOS 18+), matched by the details.
    var zoomNamespace: Namespace.ID? = nil
    var onOpen: (Challenge) -> Void = { _ in }
    var onToggleLike: (Challenge) -> Void = { _ in }
    var onRefresh: (() async -> Void)? = nil

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                ForEach(challenges, id: \.id) { challenge in
                    ChallengeCardView(
                        challenge: challenge,
                        status: statuses[challenge.id] ?? .active,
                        counts: socialCounts[challenge.id] ?? SocialCounts(likeCount: 0, commentCount: 0, likedByMe: false),
                        onOpen: { onOpen(challenge) },
                        onToggleLike: { onToggleLike(challenge) }
                    )
                    .challengeZoomSource(id: challenge.id, in: zoomNamespace)
                }
            }
            .frame(maxWidth: 700)
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity)
        }
        .background(Color(.systemGroupedBackground))
        .overlay {
            if challenges.isEmpty {
                if isLoading {
                    ProgressView().controlSize(.large)
                } else {
                    ContentUnavailableView {
                        Label(LocalizedStringKey("challenges_empty"), systemImage: "trophy")
                    } description: {
                        Text(LocalizedStringKey("challenges_empty_hint"))
                    }
                }
            }
        }
        .refreshable { await onRefresh?() }
    }
}

extension View {
    /// Marks a view as the source of the zoom transition into a challenge's details (iOS 18+; a no-op before).
    @ViewBuilder
    func challengeZoomSource(id: String, in namespace: Namespace.ID?) -> some View {
        if #available(iOS 18.0, *), let namespace {
            self.matchedTransitionSource(id: id, in: namespace) { $0.clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous)) }
        } else {
            self
        }
    }

    /// The details zoom out of the tapped card (iOS 18+; the standard push before).
    @ViewBuilder
    func challengeZoomDestination(id: String, in namespace: Namespace.ID?) -> some View {
        if #available(iOS 18.0, *), let namespace {
            self.navigationTransition(.zoom(sourceID: id, in: namespace))
        } else {
            self
        }
    }
}

private let previewChallenges = [
    ChallengePreviewData.challenge(),
    ChallengePreviewData.challenge(id: "c2", title: "5000 m Altitude Climb", description: "Teams compete.", scope: "individual", type: "elevation", target: 5_000),
]

#Preview("Light Mode") {
    NavigationStack {
        ChallengeListView(
            challenges: previewChallenges,
            statuses: ["c1": .active, "c2": .planned],
            socialCounts: ["c1": SocialCounts(likeCount: 5, commentCount: 2, likedByMe: true)]
        )
        .navigationTitle("Challenges")
    }
    .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    NavigationStack {
        ChallengeListView(challenges: previewChallenges, statuses: ["c1": .draft, "c2": .expired]).navigationTitle("Challenges")
    }
    .preferredColorScheme(.dark)
}

#Preview("Empty, iPhone SE", traits: .fixedLayout(width: 320, height: 568)) {
    NavigationStack { ChallengeListView().navigationTitle("Challenges") }
        .environment(\.dynamicTypeSize, .xxxLarge)
}
