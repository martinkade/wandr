import SwiftUI

/// A team in the Teams tab: the cover (or elevation lines), then avatar, name and invite code, and the description.
struct TeamCardView: View {
    let teamId: String
    let name: String
    var teamDescription: String? = nil
    var avatarUrl: String? = nil
    var coverUrl: String? = nil
    let inviteCode: String

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            TeamCoverView(coverUrl: coverUrl, seed: TeamCoverView.seed(for: teamId))
                .frame(height: 128)

            HStack(spacing: 16) {
                AvatarView(url: avatarUrl, name: name, size: 64)
                VStack(alignment: .leading, spacing: 2) {
                    Text(name)
                        .font(.title3.weight(.semibold))
                        .foregroundStyle(.primary)
                        .lineLimit(1)
                    Text(String(format: String(localized: "team_invite_code"), inviteCode))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
                Spacer(minLength: 0)
            }
            .padding(16)

            if let teamDescription, !teamDescription.isEmpty {
                Text(teamDescription)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                    .padding(.horizontal, 16)
                    .padding(.bottom, 16)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .wandrCard()
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .contentShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .accessibilityElement(children: .combine)
    }
}

#Preview("Light Mode") {
    TeamCardView(teamId: "t1", name: "Alpine Trail Blazers", teamDescription: "Hiking team for weekend trips.", inviteCode: "X7K9P2W1")
        .padding()
        .background(Color(.systemGroupedBackground))
        .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    TeamCardView(teamId: "t2", name: "City Runners", inviteCode: "Q3M8D5LA")
        .padding()
        .background(Color(.systemGroupedBackground))
        .preferredColorScheme(.dark)
}

#Preview("Large text") {
    TeamCardView(teamId: "t1", name: "Alpine Trail Blazers", teamDescription: "Hiking team for weekend trips.", inviteCode: "X7K9P2W1")
        .padding()
        .background(Color(.systemGroupedBackground))
        .dynamicTypeSize(.accessibility2)
}
