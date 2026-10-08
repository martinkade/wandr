@preconcurrency import shared
import SwiftUI

/// Teams the signed-in manager belongs to, with a button to create a new one.
struct TeamListView: View {
    let teams: [Team]
    var onSelectTeam: (Team) -> Void = { _ in }
    var onCreateTeam: () -> Void = {}

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            if teams.isEmpty {
                Text(LocalizedStringKey("teams_empty"))
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(32)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ScrollView {
                    LazyVStack(spacing: 12) {
                        ForEach(teams, id: \.id) { team in
                            TeamCardView(
                                name: team.name,
                                teamDescription: team.description_,
                                inviteCode: team.inviteCode,
                                onSelect: { onSelectTeam(team) }
                            )
                        }
                    }
                    .padding(16)
                    .padding(.bottom, 72) // keep the last card clear of the button
                }
            }

            Button(action: onCreateTeam) {
                Label(LocalizedStringKey("teams_new_button"), systemImage: "plus")
                    .font(.headline)
                    .foregroundColor(.wandrOnPrimary)
                    .padding(.horizontal, 20)
                    .frame(height: 50)
                    .background(Color.wandrPrimary)
                    .clipShape(Capsule())
                    .shadow(radius: 4)
            }
            .padding(16)
        }
    }
}

#Preview("Empty") { TeamListView(teams: []) }
#Preview("Empty Dark") { TeamListView(teams: []).preferredColorScheme(.dark) }
