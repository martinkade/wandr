@preconcurrency import shared
import SwiftUI

/// Read-only group details with the member list. Owners and admins get an "Edit" button that opens
/// `TeamEditView` in a sheet; saving closes it. Results (saved / image updated / error) are reported
/// in a snackbar (on the sheet while it is open).
struct TeamDetailsView: View {
    @Binding var name: String
    @Binding var teamDescription: String
    var avatarUrl: String? = nil
    var coverUrl: String? = nil
    var inviteCode: String = ""
    var members: [TeamMember] = []
    var canEdit: Bool = false
    var isSaving: Bool = false
    var isImageUpdating: Bool = false
    /// Increments after each successful save; closes the sheet.
    var saveCompletedCount: Int = 0
    @Binding var snackbar: SnackbarMessage?
    var onSave: () -> Void = {}
    var onCancel: () -> Void = {}
    var onAvatarReady: (Data) -> Void = { _ in }
    var onRemoveAvatar: () -> Void = {}
    var onCoverReady: (Data) -> Void = { _ in }
    var onRemoveCover: () -> Void = {}
    /// Only for previews.
    var startEditing: Bool = false

    @State private var isEditing = false

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                TeamCoverView(coverUrl: coverUrl)

                AvatarPickerView(displayName: name, avatarUrl: avatarUrl, isBusy: isImageUpdating, isEnabled: false)

                LabeledValueView(label: "team_name_label", value: name)
                LabeledValueView(label: "team_description_label", value: teamDescription)

                Text(String(format: String(localized: "group_invite_code"), inviteCode))
                    .font(.footnote.weight(.semibold))
                    .foregroundColor(.wandrAccentText)
                    .frame(maxWidth: .infinity, alignment: .leading)

                Divider().padding(.vertical, 4)

                Text(String(format: String(localized: "team_members_count"), members.count))
                    .font(.headline)
                    .foregroundColor(.wandrAccentText)
                    .frame(maxWidth: .infinity, alignment: .leading)

                if members.isEmpty {
                    Text(LocalizedStringKey("team_no_members"))
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                } else {
                    ForEach(members, id: \.id) { member in
                        TeamMemberRowView(
                            displayName: member.displayName,
                            username: member.username,
                            avatarUrl: member.avatarUrl,
                            isAdmin: member.role == .admin
                        )
                    }
                }
            }
            .padding(24)
        }
        .background(
            LinearGradient(
                colors: [Color.wandrPrimary.opacity(0.08), Color.wandrSecondary.opacity(0.05)],
                startPoint: .topLeading, endPoint: .bottomTrailing
            )
            .ignoresSafeArea()
        )
        .navigationTitle(LocalizedStringKey("team_details_title"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if canEdit {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(LocalizedStringKey("profile_edit_button")) { isEditing = true }
                }
            }
        }
        .snackbar($snackbar)
        .sheet(isPresented: $isEditing, onDismiss: onCancel) {
            TeamEditView(
                name: $name,
                teamDescription: $teamDescription,
                avatarUrl: avatarUrl,
                coverUrl: coverUrl,
                isSaving: isSaving,
                isImageUpdating: isImageUpdating,
                snackbar: $snackbar,
                onSave: onSave,
                onCancel: { isEditing = false },
                onAvatarReady: onAvatarReady,
                onRemoveAvatar: onRemoveAvatar,
                onCoverReady: onCoverReady,
                onRemoveCover: onRemoveCover
            )
            .presentationDetents([.large])
            .presentationDragIndicator(.visible)
            .interactiveDismissDisabled(isSaving)
        }
        .onChange(of: saveCompletedCount) { _, _ in isEditing = false }
        .onChange(of: canEdit) { _, allowed in if !allowed { isEditing = false } }
        .onAppear { if startEditing { isEditing = true } }
    }
}

/// Connects `TeamDetailsView` to the shared `TeamDetailsViewModel` for one team.
struct TeamDetailsContainerView: View {
    let teamId: String
    let userId: String
    @StateObject private var observer = TeamDetailsObserver()

    var body: some View {
        Group {
            if observer.isLoading && observer.name.isEmpty {
                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                TeamDetailsView(
                    name: Binding(get: { observer.name }, set: observer.setName),
                    teamDescription: Binding(get: { observer.teamDescription }, set: observer.setDescription),
                    avatarUrl: observer.avatarUrl,
                    coverUrl: observer.coverUrl,
                    inviteCode: observer.inviteCode,
                    members: observer.members,
                    canEdit: observer.canEdit,
                    isSaving: observer.isSaving,
                    isImageUpdating: observer.isImageUpdating,
                    saveCompletedCount: observer.saveCompletedCount,
                    snackbar: $observer.snackbar,
                    onSave: observer.save,
                    onCancel: observer.discard,
                    onAvatarReady: observer.uploadAvatar,
                    onRemoveAvatar: observer.removeAvatar,
                    onCoverReady: observer.uploadCover,
                    onRemoveCover: observer.removeCover
                )
            }
        }
        .task { observer.load(teamId: teamId, userId: userId) }
    }
}

#Preview("Read-only") {
    NavigationStack {
        TeamDetailsView(
            name: .constant("Alpine Trail Blazers"), teamDescription: .constant("Hiking group for weekend trips."),
            inviteCode: "X7K9P2W1", canEdit: true, snackbar: .constant(nil)
        )
    }
}

#Preview("Read-only Dark") {
    NavigationStack {
        TeamDetailsView(
            name: .constant("Alpine Trail Blazers"), teamDescription: .constant("Hiking group for weekend trips."),
            inviteCode: "X7K9P2W1", canEdit: true, snackbar: .constant(nil)
        )
    }
    .preferredColorScheme(.dark)
}

#Preview("With edit sheet") {
    NavigationStack {
        TeamDetailsView(
            name: .constant("Alpine Trail Blazers"), teamDescription: .constant("Hiking group for weekend trips."),
            inviteCode: "X7K9P2W1", canEdit: true, snackbar: .constant(nil), startEditing: true
        )
    }
}

#Preview("iPad, snackbar", traits: .fixedLayout(width: 820, height: 1100)) {
    NavigationStack {
        TeamDetailsView(
            name: .constant("Alpine Trail Blazers"), teamDescription: .constant(""),
            inviteCode: "X7K9P2W1", canEdit: true,
            snackbar: .constant(SnackbarMessage(text: "Group saved")), startEditing: true
        )
    }
}
