@preconcurrency import shared
import SwiftUI

/// Connects `ProfileView` to the shared ProfileViewModel and TeamViewModel for a signed-in user. Lives inside the
/// profile tab's NavigationStack (owned by the main view); teams open `TeamDetailView` there.
struct ProfileContainerView: View {
    let userId: String?
    /// Owned by the main view, which also syncs it when the app becomes active.
    @ObservedObject var healthImport: HealthKitImporter
    var onLogout: () -> Void = {}
    var onAccountDeleted: () -> Void = {
    }
    @StateObject private var observer = ProfileObserver()
    @StateObject private var teams = TeamObserver()
    @State private var showJoin = false

    var body: some View {
        ProfileView(
            displayName: Binding(get: { observer.displayName }, set: observer.setDisplayName),
            bio: Binding(get: { observer.bio }, set: observer.setBio),
            avatarUrl: observer.avatarUrl,
            memberSince: observer.memberSince,
            activityCount: observer.activityCount,
            isSaving: observer.isSaving,
            isAvatarUpdating: observer.isAvatarUpdating,
            saveCompletedCount: observer.saveCompletedCount,
            snackbar: $observer.snackbar,
            onSave: observer.save,
            onCancel: observer.discard,
            onAvatarReady: observer.uploadAvatar,
            onRemoveAvatar: observer.removeAvatar,
            onLogout: onLogout,
            userId: userId,
            onAccountDeleted: onAccountDeleted,
            healthSummary: healthImport.isEnabled ? healthImport.summary : nil,
            onHealthSettings: healthImport.openSettingsOrConnect,
            teams: teams.teams,
            onReorderTeams: teams.reorder(teamIds:),
            onJoinTeam: {
                teams.resetJoin()
                showJoin = true
            }
        )
        .snackbar($teams.snackbar)
        .navigationDestination(for: TeamRoute.self) { route in
            if let userId { TeamDetailView(teamId: route.teamId, userId: userId) }
        }
        .sheet(isPresented: $showJoin, onDismiss: teams.resetJoin) {
            JoinTeamSheetView(
                code: Binding(get: { teams.joinCode }, set: teams.setJoinCode),
                isJoining: teams.isJoining,
                errorMessage: teams.joinErrorMessage,
                onJoin: { teams.join() },
                onScanned: { raw in teams.join(code: raw) }
            )
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.visible)
            .interactiveDismissDisabled(teams.isJoining)
        }
        .onChange(of: teams.joinedCount) { _, _ in showJoin = false }
        .permissionDisclosure(
            isPresented: $healthImport.showDisclosure,
            title: "healthkit_disclosure_title",
            message: "healthkit_disclosure_message"
        ) { healthImport.disclosureConfirmed() }
        .alert(
            healthImport.message.map { String(localized: $0) } ?? "",
            isPresented: Binding(get: { healthImport.message != nil }, set: { if !$0 { healthImport.message = nil } })
        ) { Button("OK", role: .cancel) {} }
        .task(id: userId) {
            if let userId {
                observer.load(userId: userId)
                teams.load(userId: userId)
            }
        }
    }
}
