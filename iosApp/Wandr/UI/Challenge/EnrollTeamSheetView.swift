import SwiftUI
@preconcurrency import shared

/// Sheet to choose which of the user's administered teams to enroll in a team challenge. The primary team (the only
/// one that counts for the user's own progress) is marked.
struct EnrollTeamSheetView: View {
    let teams: [Team]
    let primaryTeamId: String?
    var onSelect: (Team) -> Void = { _ in }
    var onCancel: () -> Void = {}

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 2) {
                    Text(LocalizedStringKey("challenge_enroll_dialog_hint"))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .padding(.bottom, 14)
                    ForEach(Array(teams.enumerated()), id: \.element.id) { index, team in
                        Button { onSelect(team) } label: { row(team) }
                            .buttonStyle(.plain)
                            .wandrSectionItem(isFirst: index == 0, isLast: index == teams.count - 1)
                    }
                }
                .padding()
            }
            .background(Color(.systemGroupedBackground))
            .navigationTitle(LocalizedStringKey("challenge_enroll_dialog_title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(LocalizedStringKey("cancel_button"), action: onCancel)
                }
            }
        }
    }

    private func row(_ team: Team) -> some View {
        HStack(spacing: 14) {
            AvatarView(url: team.avatarUrl, name: team.name, size: 44)
            VStack(alignment: .leading, spacing: 2) {
                Text(team.name).font(.headline).lineLimit(1)
                if team.id == primaryTeamId {
                    Label(LocalizedStringKey("memberships_primary"), systemImage: "star.fill")
                        .font(.footnote)
                        .foregroundStyle(Color.wandrAccentText)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Image(systemName: "chevron.right").font(.footnote.weight(.semibold)).foregroundStyle(.tertiary)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .contentShape(Rectangle())
    }
}

#Preview("Light Mode") {
    EnrollTeamSheetView(
        teams: [ChallengePreviewData.team("t1", "Alpine Trail Blazers"), ChallengePreviewData.team("t2", "City Runners")],
        primaryTeamId: "t1"
    )
    .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    EnrollTeamSheetView(teams: [ChallengePreviewData.team("t1", "Alpine Trail Blazers")], primaryTeamId: nil)
        .preferredColorScheme(.dark)
}

#Preview("Large Text, iPhone SE", traits: .fixedLayout(width: 320, height: 568)) {
    EnrollTeamSheetView(teams: [ChallengePreviewData.team("t1", "Alpine Trail Blazers")], primaryTeamId: "t1")
        .environment(\.dynamicTypeSize, .accessibility2)
}
