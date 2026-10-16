@preconcurrency import shared
import SwiftUI

/// "Teams" tab for managers: the list and a sheet to create a team, connected to the shared TeamViewModel.
/// Owns its NavigationStack (list -> `TeamDetailView`).
struct TeamsContainerView: View {
    let userId: String?
    @StateObject private var observer = TeamObserver()
    @State private var showCreate = false
    @State private var newName = ""
    @State private var newDescription = ""

    var body: some View {
        NavigationStack {
            TeamListView(teams: observer.teams, onCreateTeam: openCreate)
                .navigationTitle(LocalizedStringKey("tab_teams"))
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button(action: openCreate) { Image(systemName: "plus") }
                            .accessibilityLabel(Text(LocalizedStringKey("teams_new_button")))
                    }
                }
                .navigationDestination(for: TeamRoute.self) { route in
                    if let userId { TeamDetailView(teamId: route.teamId, userId: userId) }
                }
                .snackbar(showCreate ? .constant(nil) : $observer.snackbar)
        }
        .sheet(isPresented: $showCreate) {
            TeamEditView(
                title: "create_team_title",
                saveTitle: "create_team_button",
                name: $newName,
                teamDescription: $newDescription,
                isSaving: observer.isCreating,
                snackbar: $observer.snackbar,
                onSave: { observer.create(name: newName, description: newDescription) },
                onCancel: { showCreate = false }
            )
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.visible)
            .interactiveDismissDisabled(observer.isCreating)
        }
        .onChange(of: observer.createdCount) { _, _ in showCreate = false }
        .task(id: userId) {
            if let userId { observer.load(userId: userId) }
        }
    }

    private func openCreate() {
        newName = ""
        newDescription = ""
        observer.snackbar = nil
        showCreate = true
    }
}
