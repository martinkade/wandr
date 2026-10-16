@preconcurrency import shared
import SwiftUI

/// The manager's teams as cards; each card pushes `TeamRoute`. A floating glass button creates a new team.
struct TeamListView: View {
    let teams: [Team]
    var onCreateTeam: () -> Void = {}

    var body: some View {
        Group {
            if teams.isEmpty {
                ContentUnavailableView {
                    Label(LocalizedStringKey("tab_teams"), systemImage: "person.3")
                } description: {
                    Text(LocalizedStringKey("teams_empty"))
                } actions: {
                    Button(action: onCreateTeam) {
                        Label(LocalizedStringKey("teams_new_button"), systemImage: "plus")
                    }
                    .wandrProminentButton()
                }
            } else {
                ScrollView {
                    LazyVStack(spacing: 16) {
                        ForEach(teams, id: \.id) { team in
                            NavigationLink(value: TeamRoute(teamId: team.id)) {
                                TeamCardView(
                                    teamId: team.id,
                                    name: team.name,
                                    teamDescription: team.description_,
                                    avatarUrl: team.avatarUrl,
                                    coverUrl: team.coverUrl,
                                    inviteCode: team.inviteCode
                                )
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(16)
                    .padding(.bottom, 72) // keep the last card clear of the floating button
                }
                .overlay(alignment: .bottomTrailing) {
                    Button(action: onCreateTeam) {
                        Label(LocalizedStringKey("teams_new_button"), systemImage: "plus")
                            .font(.headline)
                            .padding(.horizontal, 8)
                            .frame(minHeight: 40)
                    }
                    .wandrProminentButton()
                    .controlSize(.large)
                    .padding(16)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemGroupedBackground).ignoresSafeArea())
    }
}

#Preview("Light Mode") {
    NavigationStack { TeamListView(teams: TeamPreviewData.teams).navigationTitle("Teams") }
        .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    NavigationStack { TeamListView(teams: TeamPreviewData.teams).navigationTitle("Teams") }
        .preferredColorScheme(.dark)
}

#Preview("Empty, large text") {
    NavigationStack { TeamListView(teams: []).navigationTitle("Teams") }
        .dynamicTypeSize(.accessibility1)
}
