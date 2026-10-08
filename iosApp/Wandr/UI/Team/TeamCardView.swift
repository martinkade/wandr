@preconcurrency import shared
import SwiftUI

struct TeamCardView: View {
    let name: String
    var teamDescription: String? = nil
    let inviteCode: String
    var onSelect: () -> Void = {}

    var body: some View {
        Button(action: onSelect) {
            VStack(alignment: .leading, spacing: 8) {
                Text(name)
                    .font(.headline)
                    .foregroundColor(.primary)

                if let teamDescription, !teamDescription.isEmpty {
                    Text(teamDescription)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.leading)
                }

                Text(String(format: String(localized: "team_invite_code"), inviteCode))
                    .font(.caption.weight(.semibold))
                    .foregroundColor(.wandrAccentText)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding()
            .background(.ultraThinMaterial)
            .cornerRadius(16)
        }
        .buttonStyle(.plain)
    }
}

#Preview("Light Mode") {
    TeamCardView(name: "Alpine Trail Blazers", teamDescription: "Hiking team for weekend trips.", inviteCode: "X7K9P2W1")
        .padding()
        .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    TeamCardView(name: "Alpine Trail Blazers", teamDescription: "Hiking team for weekend trips.", inviteCode: "X7K9P2W1")
        .padding()
        .preferredColorScheme(.dark)
}

#Preview("iPad", traits: .fixedLayout(width: 700, height: 160)) {
    TeamCardView(name: "City Runners", inviteCode: "Q3M8D5LA").padding()
}
