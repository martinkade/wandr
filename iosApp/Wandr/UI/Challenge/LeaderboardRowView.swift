import SwiftUI
@preconcurrency import shared

/// One member in the ranking of a challenge: rank, avatar, name and progress. The signed-in user's row is highlighted.
struct LeaderboardRowView: View {
    let entry: LeaderboardEntry
    let type: ChallengeType
    var isMe: Bool = false
    var isFirst: Bool = true
    var isLast: Bool = true

    private var name: String {
        entry.displayName.trimmingCharacters(in: .whitespaces).isEmpty ? String(localized: "social_unknown_author") : entry.displayName
    }

    var body: some View {
        HStack(spacing: 14) {
            Text("#\(entry.rank)")
                .font(.headline.monospacedDigit())
                .foregroundStyle(Color.wandrAccentText)
            AvatarView(url: entry.avatarUrl, name: name, size: 40)
            VStack(alignment: .leading, spacing: 6) {
                HStack(alignment: .firstTextBaseline, spacing: 12) {
                    Text(isMe ? "\(name) · \(String(localized: "challenge_ranking_you"))" : name)
                        .font(.headline.weight(isMe ? .bold : .regular))
                        .lineLimit(1)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    if entry.isCompleted {
                        Image(systemName: "checkmark.seal.fill").foregroundStyle(Color.wandrAccentText)
                    }
                    Text(ChallengeText.value(type, entry.progressValue))
                        .font(.subheadline.weight(.semibold).monospacedDigit())
                        .foregroundStyle(Color.wandrSecondary)
                }
                HStack(spacing: 12) {
                    ProgressView(value: min(max(entry.progressPercentage / 100, 0), 1)).tint(Color.wandrPrimary)
                    Text("\(Int(entry.progressPercentage))%")
                        .font(.footnote.monospacedDigit())
                        .foregroundStyle(.secondary)
                }
            }
        }
        .challengeRankingRow(isFirst: isFirst, isLast: isLast, isHighlighted: isMe)
        .accessibilityElement(children: .combine)
    }
}

#Preview("Light Mode") {
    let entries = ChallengePreviewData.leaderboard
    VStack(spacing: 2) {
        ForEach(Array(entries.enumerated()), id: \.offset) { index, entry in
            LeaderboardRowView(entry: entry, type: .distance, isMe: entry.userId == "u1", isFirst: index == 0, isLast: index == entries.count - 1)
        }
    }
    .padding()
    .background(Color(.systemGroupedBackground))
    .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    LeaderboardRowView(entry: ChallengePreviewData.leaderboard[1], type: .distance, isMe: true)
        .padding()
        .background(Color(.systemGroupedBackground))
        .preferredColorScheme(.dark)
}

#Preview("Large Text", traits: .fixedLayout(width: 375, height: 400)) {
    LeaderboardRowView(entry: ChallengePreviewData.leaderboard[3], type: .time)
        .padding()
        .environment(\.dynamicTypeSize, .accessibility2)
}
