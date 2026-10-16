import SwiftUI
@preconcurrency import shared

/// The challenge details, built from plain values (the observer lives in `ChallengeDetailView`). A stretchy cover with
/// parallax sits behind a transparent navigation bar; the content sheet slides over its lower edge with the emblem on
/// the seam. Once the cover has scrolled away, the bar turns solid and shows the title.
struct ChallengeDetailContentView<Social: View>: View {
    let challenge: Challenge
    let status: ChallengeStatus
    let userId: String
    var action: ChallengeAction? = nil
    var standings: [TeamStanding] = []
    var leaderboard: [LeaderboardEntry] = []
    var primaryTeamId: String? = nil
    var canEdit: Bool = false
    var isImageUpdating: Bool = false
    var onAction: (ChallengeAction) -> Void = { _ in }
    var onEdit: () -> Void = {}
    var onChangeCover: () -> Void = {}
    @ViewBuilder var social: () -> Social

    @State private var isCollapsed = false

    private let headerHeight: CGFloat = 280
    private let sheetOverlap: CGFloat = 28
    private let badgeSize: CGFloat = 88

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                header
                sheet
                    .padding(.top, -sheetOverlap)
            }
        }
        .ignoresSafeArea(edges: .top)
        .background(Color(.systemGroupedBackground))
        .navigationTitle(isCollapsed ? challenge.title : "")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(isCollapsed ? .visible : .hidden, for: .navigationBar)
        .animation(.easeInOut(duration: 0.2), value: isCollapsed)
        .toolbar {
            if canEdit {
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        Button(action: onEdit) { Label(LocalizedStringKey("profile_edit_button"), systemImage: "pencil") }
                        Button(action: onChangeCover) { Label(LocalizedStringKey("challenge_change_cover"), systemImage: "photo") }
                    } label: {
                        Label(LocalizedStringKey("challenge_more_options"), systemImage: "ellipsis")
                    }
                    .disabled(isImageUpdating)
                }
            }
        }
    }

    // MARK: - Cover

    /// Pulled down, the cover stretches; scrolled up, it moves at a slower pace than the content (parallax).
    private var header: some View {
        GeometryReader { geo in
            let minY = geo.frame(in: .scrollView).minY
            ChallengeCoverView(coverUrl: challenge.coverUrl, seed: challenge.contourSeed, isBusy: isImageUpdating, showsTopScrim: true)
                .frame(width: geo.size.width, height: headerHeight + max(0, minY))
                .offset(y: minY > 0 ? -minY : -minY * 0.45)
                .onChange(of: minY < -(headerHeight - 120)) { _, collapsed in isCollapsed = collapsed }
        }
        .frame(height: headerHeight)
        .contentShape(Rectangle())
        .onTapGesture { if canEdit && !isImageUpdating { onChangeCover() } }
    }

    // MARK: - Sheet

    private var sheet: some View {
        VStack(spacing: 0) {
            VStack(spacing: 8) {
                Text(challenge.title)
                    .font(.title2.weight(.bold))
                    .multilineTextAlignment(.center)
                if status == .draft {
                    Label(LocalizedStringKey("challenge_draft_hint"), systemImage: "eye.slash")
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(Color.wandrAccentText)
                }
                if let description = challenge.description_, !description.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    Text(description)
                        .font(.body)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.top, 4)
                }
            }
            .padding(.horizontal, 8)

            infoCard.padding(.top, 24)

            if let action {
                ChallengeActionButtonView(action: action) { onAction(action) }
                    .padding(.top, 20)
            }

            if challenge.isTeamChallenge { standingsSection.padding(.top, 32) }

            // Who is ahead: everybody in an individual challenge, the user's own team in a team challenge.
            if !leaderboard.isEmpty { rankingSection.padding(.top, 32) }

            social()
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.top, 28)
        }
        .frame(maxWidth: 700)
        .padding(.horizontal, 16)
        .padding(.top, badgeSize / 2 + 14)
        .padding(.bottom, 32)
        .frame(maxWidth: .infinity)
        .background(
            Color(.systemGroupedBackground),
            in: UnevenRoundedRectangle(topLeadingRadius: sheetOverlap, topTrailingRadius: sheetOverlap, style: .continuous)
        )
        .overlay(alignment: .top) {
            ChallengeBadgeView(type: challenge.challengeType, size: badgeSize)
                .offset(y: -badgeSize / 2)
        }
    }

    private var infoCard: some View {
        VStack(spacing: 18) {
            ChallengeInfoRowView(
                systemImage: "calendar",
                text: ChallengeText.lengthAndRemaining(challenge, status: status),
                secondaryText: ChallengeText.period(challenge)
            )
            ChallengeInfoRowView(systemImage: "flag.fill", text: ChallengeText.goal(challenge))
            ChallengeInfoRowView(systemImage: "figure.run", text: ChallengeText.activityTypes(challenge.activityTypes))
            ChallengeInfoRowView(
                systemImage: "trophy.fill",
                text: String(localized: challenge.isTeamChallenge ? "challenge_scope_team" : "challenge_scope_individual"),
                secondaryText: challenge.isTeamChallenge && challenge.requireAllMembersCompletion
                    ? String(localized: "require_all_members_label") : nil
            )
        }
        .padding(18)
        .wandrCard()
    }

    private var standingsSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionTitle("challenge_standings_title")
            if standings.isEmpty {
                Text(LocalizedStringKey("challenge_standings_empty"))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(16)
                    .wandrCard()
            } else {
                VStack(spacing: 2) {
                    ForEach(Array(standings.enumerated()), id: \.element.teamId) { index, standing in
                        TeamStandingRowView(
                            standing: standing,
                            type: challenge.challengeType,
                            isFirst: index == 0,
                            isLast: index == standings.count - 1,
                            isMine: standing.teamId == primaryTeamId,
                            showMemberProgress: challenge.requireAllMembersCompletion
                        )
                    }
                }
            }
        }
    }

    private var rankingSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionTitle(challenge.isTeamChallenge ? "challenge_team_ranking_title" : "challenge_ranking_title")
            VStack(spacing: 2) {
                ForEach(Array(leaderboard.enumerated()), id: \.element.userId) { index, entry in
                    LeaderboardRowView(
                        entry: entry,
                        type: challenge.challengeType,
                        isMe: entry.userId == userId,
                        isFirst: index == 0,
                        isLast: index == leaderboard.count - 1
                    )
                }
            }
        }
    }

    private func sectionTitle(_ key: LocalizedStringKey) -> some View {
        Text(key)
            .font(.title3.weight(.semibold))
            .padding(.horizontal, 4)
            .accessibilityAddTraits(.isHeader)
    }
}

#Preview("Team, creator") {
    NavigationStack {
        ChallengeDetailContentView(
            challenge: ChallengePreviewData.challenge(type: "elevation", target: 5_000),
            status: .active,
            userId: "u1",
            action: .withdrawTeam,
            standings: ChallengePreviewData.standings,
            leaderboard: ChallengePreviewData.leaderboard,
            primaryTeamId: "t1",
            canEdit: true
        ) {
            SocialBarView(likeCount: 12, likedByMe: true, commentCount: 3)
        }
    }
    .preferredColorScheme(.light)
}

#Preview("Individual draft, Dark") {
    NavigationStack {
        ChallengeDetailContentView(
            challenge: ChallengePreviewData.challenge(scope: "individual", isActive: false, activityTypes: ["hiking", "running"]),
            status: .draft,
            userId: "u3",
            leaderboard: Array(ChallengePreviewData.leaderboard.prefix(2))
        ) {
            SocialBarView(likeCount: 0, likedByMe: false, commentCount: 0)
        }
    }
    .preferredColorScheme(.dark)
}

#Preview("Large Text, iPhone SE", traits: .fixedLayout(width: 320, height: 568)) {
    NavigationStack {
        ChallengeDetailContentView(
            challenge: ChallengePreviewData.challenge(),
            status: .planned,
            userId: "u1",
            action: .enrollTeam,
            isImageUpdating: true
        ) {
            SocialBarView(likeCount: 1, likedByMe: false, commentCount: 0)
        }
    }
    .environment(\.dynamicTypeSize, .accessibility1)
}
