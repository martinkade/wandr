import SwiftUI

/// Trailing row of "My teams" that starts joining another team.
struct JoinTeamRowView: View {
    var onJoin: () -> Void = {}

    var body: some View {
        Button(action: onJoin) {
            HStack(spacing: 16) {
                Image(systemName: "plus")
                    .font(.headline)
                    .frame(width: 40, height: 40)
                Text(LocalizedStringKey("memberships_join_item"))
                    .font(.headline)
                Spacer(minLength: 0)
            }
            .foregroundStyle(Color.wandrAccentText)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

#Preview("Light Mode") {
    List { JoinTeamRowView() }.preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    List { JoinTeamRowView() }.preferredColorScheme(.dark)
}

#Preview("Large text") {
    List { JoinTeamRowView() }.dynamicTypeSize(.accessibility3)
}
