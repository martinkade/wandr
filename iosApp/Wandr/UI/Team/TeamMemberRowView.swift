import SwiftUI

/// A member as one card of a list of connected cards; admins get a role chip.
struct TeamMemberRowView: View {
    let displayName: String
    let username: String
    var avatarUrl: String? = nil
    let isAdmin: Bool
    var isFirst: Bool = true
    var isLast: Bool = true

    var body: some View {
        let name = displayName.isEmpty ? username : displayName
        HStack(spacing: 16) {
            AvatarView(url: avatarUrl, name: name, size: 40)

            VStack(alignment: .leading, spacing: 2) {
                Text(name)
                    .font(.headline)
                    .foregroundStyle(.primary)
                    .lineLimit(1)
                if !username.isEmpty {
                    Text("@\(username)")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }

            Spacer(minLength: 0)

            if isAdmin {
                Text(LocalizedStringKey("role_admin"))
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Color.wandrOnPrimary)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(Color.wandrPrimary, in: Capsule())
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .wandrSectionItem(isFirst: isFirst, isLast: isLast)
        .accessibilityElement(children: .combine)
    }
}

#Preview("Light Mode") {
    VStack(spacing: 2) {
        TeamMemberRowView(displayName: "Martin Kade", username: "martinkade", isAdmin: true, isLast: false)
        TeamMemberRowView(displayName: "Alex Runner", username: "runner99", isAdmin: false, isFirst: false)
    }
    .padding()
    .background(Color(.systemGroupedBackground))
    .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    VStack(spacing: 2) {
        TeamMemberRowView(displayName: "Martin Kade", username: "martinkade", isAdmin: true, isLast: false)
        TeamMemberRowView(displayName: "", username: "runner99", isAdmin: false, isFirst: false)
    }
    .padding()
    .background(Color(.systemGroupedBackground))
    .preferredColorScheme(.dark)
}

#Preview("Large text") {
    TeamMemberRowView(displayName: "Sophie Hiker", username: "sophie", isAdmin: true)
        .padding()
        .background(Color(.systemGroupedBackground))
        .dynamicTypeSize(.accessibility2)
}
