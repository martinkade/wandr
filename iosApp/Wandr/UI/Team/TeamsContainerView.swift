@preconcurrency import shared
import SwiftUI

/// "Teams" tab for managers: the list plus a sheet to create a new team, connected to the shared TeamViewModel.
struct TeamsContainerView: View {
    let userId: String?
    @StateObject private var observer = TeamObserver()
    @State private var showCreate = false
    @State private var selectedTeamId: String?

    var body: some View {
        NavigationStack {
            TeamListView(
                teams: observer.teams,
                onSelectTeam: { selectedTeamId = $0.id },
                onCreateTeam: {
                    observer.resetMessages()
                    showCreate = true
                }
            )
            .navigationTitle(LocalizedStringKey("tab_teams"))
            .navigationDestination(item: $selectedTeamId) { teamId in
                if let userId { TeamDetailsContainerView(teamId: teamId, userId: userId) }
            }
        }
        .sheet(isPresented: $showCreate) {
            CreateTeamView(
                isLoading: observer.isLoading,
                errorMessage: observer.errorMessage,
                onCreateTeam: { name, description in observer.create(name: name, description: description) }
            )
        }
        .onChange(of: observer.createdCount) { _, _ in showCreate = false }
        .task(id: userId) {
            if let userId { observer.load(userId: userId) }
        }
    }
}
