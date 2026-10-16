import SwiftUI
@preconcurrency import shared

/// A challenge in the list, built like the details: cover (or elevation lines) with a glass status chip, the emblem,
/// title, length and remaining time, description and the key facts, plus likes and comments. Tapping opens the
/// details; joining and leaving happen there only. The comments live on the details, so their button opens them too.
struct ChallengeCardView: View {
    let challenge: Challenge
    /// Derived at runtime from the dates, never stored.
    let status: ChallengeStatus
    var counts: SocialCounts = SocialCounts(likeCount: 0, commentCount: 0, likedByMe: false)
    var onOpen: () -> Void = {}
    var onToggleLike: () -> Void = {}

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            ChallengeCoverView(coverUrl: challenge.coverUrl, seed: challenge.contourSeed)
                .frame(height: 128)
                .frame(maxWidth: .infinity)
                .overlay(alignment: .topTrailing) {
                    ChallengeStatusChipView(status: status).padding(12)
                }

            HStack(spacing: 14) {
                ChallengeBadgeView(type: challenge.challengeType, size: 60)
                VStack(alignment: .leading, spacing: 2) {
                    Text(challenge.title)
                        .font(.title3.weight(.semibold))
                        .lineLimit(2)
                    Text(ChallengeText.lengthAndRemaining(challenge, status: status))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(.horizontal, 16)

            if let description = challenge.description_, !description.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                Text(description)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
                    .padding(.horizontal, 16)
            }

            VStack(alignment: .leading, spacing: 6) {
                fact("flag.fill", Text(ChallengeText.goal(challenge)))
                fact("trophy.fill", Text(ChallengeText.scopeLabel(isTeam: challenge.isTeamChallenge)))
            }
            .padding(.horizontal, 16)

            SocialBarView(
                likeCount: Int(counts.likeCount),
                likedByMe: counts.likedByMe,
                commentCount: Int(counts.commentCount),
                onToggleLike: onToggleLike,
                onOpenComments: onOpen
            )
            .padding(.horizontal, 12)
            .padding(.bottom, 12)
        }
        .wandrCard()
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .contentShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        // A tap gesture instead of a Button: the like / comment buttons inside must stay separately tappable.
        .onTapGesture(perform: onOpen)
        .accessibilityElement(children: .contain)
        .accessibilityAddTraits(.isButton)
        .accessibilityAction(named: Text(challenge.title), onOpen)
    }

    private func fact(_ systemImage: String, _ text: Text) -> some View {
        Label { text } icon: {
            Image(systemName: systemImage).foregroundStyle(Color.wandrAccentText)
        }
        .font(.subheadline)
        .foregroundStyle(.secondary)
    }
}

#Preview("Light Mode") {
    ScrollView {
        ChallengeCardView(
            challenge: ChallengePreviewData.challenge(),
            status: .active,
            counts: SocialCounts(likeCount: 12, commentCount: 4, likedByMe: true)
        )
        .padding()
    }
    .background(Color(.systemGroupedBackground))
    .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    ScrollView {
        VStack(spacing: 16) {
            ChallengeCardView(
                challenge: ChallengePreviewData.challenge(id: "c2", title: "Altitude Climb", description: nil, scope: "individual", type: "elevation", target: 5_000),
                status: .draft
            )
            ChallengeCardView(
                challenge: ChallengePreviewData.challenge(id: "c3", title: "Marathon Hours", type: "time", target: 36_000),
                status: .planned
            )
        }
        .padding()
    }
    .background(Color(.systemGroupedBackground))
    .preferredColorScheme(.dark)
}

#Preview("Large Text, iPhone SE", traits: .fixedLayout(width: 320, height: 800)) {
    ScrollView {
        ChallengeCardView(challenge: ChallengePreviewData.challenge(), status: .expired).padding()
    }
    .background(Color(.systemGroupedBackground))
    .environment(\.dynamicTypeSize, .accessibility1)
}
