@preconcurrency import shared
import SwiftUI

/// The profile: header (avatar changeable by tap), stats with "Edit" (opens `ProfileEditView` in a sheet), the Apple
/// Health cards and "My teams" in priority order. Teams are reordered by dragging (after "Reorder"); only the first
/// counts for team challenges. A team pushes `TeamRoute`; the trailing row starts joining another team.
/// Results (saved / photo updated / error) are reported in a snackbar (on the sheet while it is open).
struct ProfileView: View {
    @Binding var displayName: String
    @Binding var bio: String
    var avatarUrl: String? = nil
    var memberSince: Date? = nil
    var activityCount: Int = 0
    var isSaving: Bool = false
    var isAvatarUpdating: Bool = false
    /// Increments after each successful save; closes the sheet.
    var saveCompletedCount: Int = 0
    @Binding var snackbar: SnackbarMessage?
    var onSave: () -> Void = {}
    var onCancel: () -> Void = {}
    var onAvatarReady: (Data) -> Void = { _ in }
    var onRemoveAvatar: () -> Void = {}
    var onLogout: () -> Void = {}
    var healthSummary: DailyHealthSummary? = nil
    var onHealthSettings: () -> Void = {}
    /// The user's teams, highest priority first.
    var teams: [Team] = []
    /// The new priority order (team ids, highest first).
    var onReorderTeams: ([String]) -> Void = { _ in }
    var onJoinTeam: () -> Void = {}
    /// Only for previews.
    var startEditing: Bool = false

    @State private var isEditing = false
    @State private var editMode: EditMode = .inactive
    /// Follows `teams`, but moves at once while dragging (the view model's answer arrives a moment later).
    @State private var orderedTeams: [Team] = []

    var body: some View {
        List {
            Section {
                ProfileHeaderView(
                    displayName: displayName,
                    bio: bio,
                    avatarUrl: avatarUrl,
                    isAvatarUpdating: isAvatarUpdating,
                    onAvatarReady: onAvatarReady,
                    onRemoveAvatar: onRemoveAvatar
                )
                .padding(.top, 8)
                .clearListRow()

                ProfileStatsRowView(memberSince: memberSince, activityCount: activityCount, onEdit: { isEditing = true })
                    .clearListRow()
            }

            Section {
                // Cards side by side; the stats card only appears once Apple Health is connected.
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 16) {
                        HealthImportCardView(onSettings: onHealthSettings)
                            .frame(width: 280, height: 220)
                        if let healthSummary {
                            HealthStatsCardView(summary: healthSummary)
                                .frame(width: 280, height: 220)
                        }
                    }
                }
                .scrollClipDisabled()
                .clearListRow()
            } header: {
                Text(LocalizedStringKey("health_card_title_apple")).textCase(nil)
            }

            teamsSection
        }
        .listStyle(.insetGrouped)
        .listSectionSpacing(.compact)
        .environment(\.editMode, $editMode)
        .navigationTitle(LocalizedStringKey("user_profile_title"))
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button(LocalizedStringKey("logout_button"), role: .destructive, action: onLogout)
            }
            ToolbarItem(placement: .topBarTrailing) {
                NavigationLink {
                    SettingsView()
                } label: {
                    Image(systemName: "gearshape")
                }
                .accessibilityLabel(Text(LocalizedStringKey("settings_open")))
            }
        }
        .snackbar($snackbar)
        .sheet(isPresented: $isEditing, onDismiss: onCancel) {
            ProfileEditView(
                displayName: $displayName,
                bio: $bio,
                avatarUrl: avatarUrl,
                isSaving: isSaving,
                isAvatarUpdating: isAvatarUpdating,
                snackbar: $snackbar,
                onSave: onSave,
                onCancel: { isEditing = false },
                onAvatarReady: onAvatarReady,
                onRemoveAvatar: onRemoveAvatar
            )
            .presentationDetents([.large])
            .presentationDragIndicator(.visible)
            .interactiveDismissDisabled(isSaving)
        }
        .onChange(of: saveCompletedCount) { _, _ in isEditing = false }
        .onAppear {
            orderedTeams = teams
            if startEditing { isEditing = true }
        }
        .onChange(of: teams) { _, newTeams in
            orderedTeams = newTeams
            if newTeams.count < 2 { editMode = .inactive }
        }
    }

    private var teamsSection: some View {
        Section {
            ForEach(Array(orderedTeams.enumerated()), id: \.element.id) { index, team in
                NavigationLink(value: TeamRoute(teamId: team.id)) {
                    TeamMembershipRowView(name: team.name, avatarUrl: team.avatarUrl, isPrimary: index == 0)
                }
                .deleteDisabled(true)
            }
            .onMove(perform: moveTeams)

            if !editMode.isEditing {
                JoinTeamRowView(onJoin: onJoinTeam)
            }
        } header: {
            HStack {
                Text(LocalizedStringKey("memberships_title"))
                Spacer()
                if teams.count > 1 {
                    Button(LocalizedStringKey(editMode.isEditing ? "memberships_reorder_done" : "memberships_reorder")) {
                        withAnimation { editMode = editMode.isEditing ? .inactive : .active }
                    }
                    .font(.subheadline.weight(.semibold))
                }
            }
            .textCase(nil)
        } footer: {
            if teams.count > 1 {
                Text(LocalizedStringKey("memberships_hint"))
            }
        }
    }

    private func moveTeams(from source: IndexSet, to destination: Int) {
        orderedTeams.move(fromOffsets: source, toOffset: destination)
        let ids = orderedTeams.map(\.id)
        if ids != teams.map(\.id) { onReorderTeams(ids) }
    }
}

private extension View {
    /// A list row without its own background or insets, for content that brings its own card.
    func clearListRow() -> some View {
        listRowInsets(EdgeInsets(top: 6, leading: 0, bottom: 6, trailing: 0))
            .listRowBackground(Color.clear)
            .listRowSeparator(.hidden)
    }
}

#Preview("Light Mode") {
    NavigationStack {
        ProfileView(
            displayName: .constant("Martin Kade"), bio: .constant("Outdoor hiker & developer."),
            memberSince: Date(timeIntervalSince1970: 1_768_435_200), activityCount: 42,
            snackbar: .constant(nil), teams: TeamPreviewData.teams
        )
    }
    .preferredColorScheme(.light)
}

#Preview("Dark Mode, health") {
    NavigationStack {
        ProfileView(
            displayName: .constant("Martin Kade"), bio: .constant(""), snackbar: .constant(nil),
            healthSummary: DailyHealthSummary(steps: 8432, floors: 12), teams: Array(TeamPreviewData.teams.prefix(1))
        )
    }
    .preferredColorScheme(.dark)
}

#Preview("Large text, no teams") {
    NavigationStack {
        ProfileView(displayName: .constant("Martin Kade"), bio: .constant("Outdoor hiker & developer."),
                    snackbar: .constant(SnackbarMessage(text: "Profile saved")))
    }
    .dynamicTypeSize(.accessibility2)
}

#Preview("With edit sheet") {
    NavigationStack {
        ProfileView(displayName: .constant("Martin Kade"), bio: .constant("Outdoor hiker & developer."),
                    snackbar: .constant(nil), startEditing: true)
    }
}
