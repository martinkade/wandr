import SwiftUI

/// One of the user's teams in the profile. The first one (highest priority) is marked: it is the only team that
/// counts for team challenges.
struct TeamMembershipRowView: View {
    let name: String
    var avatarUrl: String? = nil
    var isPrimary: Bool = false

    var body: some View {
        HStack(spacing: 16) {
            AvatarView(url: avatarUrl, name: name, size: 40)
            VStack(alignment: .leading, spacing: 2) {
                Text(name)
                    .font(.headline)
                    .foregroundStyle(.primary)
                    .lineLimit(1)
                if isPrimary {
                    Label(LocalizedStringKey("memberships_primary"), systemImage: "trophy.fill")
                        .labelStyle(.titleAndIcon)
                        .font(.subheadline)
                        .foregroundStyle(Color.wandrAccentText)
                        .lineLimit(1)
                }
            }
        }
        .padding(.vertical, 4)
        .accessibilityElement(children: .combine)
    }
}

#Preview("Light Mode") {
    List {
        TeamMembershipRowView(name: "Alpine Trail Blazers", isPrimary: true)
        TeamMembershipRowView(name: "City Runners")
    }
    .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    List {
        TeamMembershipRowView(name: "Alpine Trail Blazers", isPrimary: true)
        TeamMembershipRowView(name: "City Runners")
    }
    .preferredColorScheme(.dark)
}

#Preview("Large text") {
    List { TeamMembershipRowView(name: "Alpine Trail Blazers", isPrimary: true) }
        .dynamicTypeSize(.accessibility2)
}
