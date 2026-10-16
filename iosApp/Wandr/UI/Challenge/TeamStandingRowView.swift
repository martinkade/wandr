import SwiftUI
@preconcurrency import shared

/// One team in the team-vs-team ranking of a team challenge. The user's own (primary) team is highlighted.
/// `showMemberProgress` ("3 / 5"): only for all-or-nothing challenges, where it decides whether the team completes.
struct TeamStandingRowView: View {
    let standing: TeamStanding
    let type: ChallengeType
    var isFirst: Bool = true
    var isLast: Bool = true
    var isMine: Bool = false
    var showMemberProgress: Bool = false

    private var progress: Double {
        if showMemberProgress {
            return standing.memberCount > 0 ? Double(standing.completedMemberCount) / Double(standing.memberCount) : 0
        }
        return standing.progressPercentage / 100
    }

    private var progressLabel: String {
        showMemberProgress
            ? "\(standing.completedMemberCount) / \(standing.memberCount)"
            : "\(Int(standing.progressPercentage))%"
    }

    var body: some View {
        HStack(spacing: 14) {
            Text("#\(standing.rank)")
                .font(.headline.monospacedDigit())
                .foregroundStyle(Color.wandrAccentText)
            AvatarView(url: standing.avatarUrl, name: standing.teamName, size: 40)
            VStack(alignment: .leading, spacing: 6) {
                HStack(alignment: .firstTextBaseline, spacing: 12) {
                    Text(standing.teamName)
                        .font(.headline.weight(isMine ? .bold : .regular))
                        .lineLimit(1)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Text(ChallengeText.value(type, standing.totalProgress))
                        .font(.subheadline.weight(.semibold).monospacedDigit())
                        .foregroundStyle(Color.wandrSecondary)
                }
                HStack(spacing: 12) {
                    ProgressView(value: min(max(progress, 0), 1)).tint(Color.wandrPrimary)
                    Text(progressLabel)
                        .font(.footnote.monospacedDigit())
                        .foregroundStyle(.secondary)
                }
            }
        }
        .challengeRankingRow(isFirst: isFirst, isLast: isLast, isHighlighted: isMine)
        .accessibilityElement(children: .combine)
    }
}

#Preview("Light Mode") {
    VStack(spacing: 2) {
        TeamStandingRowView(standing: ChallengePreviewData.standings[0], type: .elevation, isFirst: true, isLast: false)
        TeamStandingRowView(standing: ChallengePreviewData.standings[1], type: .elevation, isFirst: false, isLast: true, isMine: true)
    }
    .padding()
    .background(Color(.systemGroupedBackground))
    .preferredColorScheme(.light)
}

#Preview("Dark Mode, all-or-nothing") {
    TeamStandingRowView(standing: ChallengePreviewData.standings[0], type: .distance, isMine: true, showMemberProgress: true)
        .padding()
        .background(Color(.systemGroupedBackground))
        .preferredColorScheme(.dark)
}

#Preview("Large Text", traits: .fixedLayout(width: 375, height: 400)) {
    TeamStandingRowView(standing: ChallengePreviewData.standings[1], type: .time)
        .padding()
        .environment(\.dynamicTypeSize, .accessibility2)
}
