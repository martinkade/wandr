@preconcurrency import shared
import SwiftUI

/// A team's details, connected to the shared TeamDetailsViewModel. Push it onto a NavigationStack (Teams tab, profile).
/// Owners and admins edit name / description in a sheet and change cover and photo right here.
struct TeamDetailView: View {
    let teamId: String
    let userId: String

    @StateObject private var observer = TeamDetailsObserver()
    @State private var isEditing = false
    @State private var showInvite = false
    @State private var showCoverFlow = false
    @State private var showAvatarFlow = false

    var body: some View {
        Group {
            if observer.isLoading && observer.name.isEmpty {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color(.systemGroupedBackground).ignoresSafeArea())
            } else {
                TeamDetailContentView(
                    teamId: teamId,
                    name: observer.name,
                    teamDescription: observer.teamDescription,
                    avatarUrl: observer.avatarUrl,
                    coverUrl: observer.coverUrl,
                    members: observer.members,
                    canEdit: observer.canEdit,
                    isImageUpdating: observer.isImageUpdating,
                    onInvite: { showInvite = true },
                    onEdit: { isEditing = true },
                    onChangeCover: { showCoverFlow = true },
                    onChangePhoto: { showAvatarFlow = true }
                )
            }
        }
        // While the edit sheet is open, it shows the messages itself.
        .snackbar(isEditing ? .constant(nil) : $observer.snackbar)
        .imagePickerFlow(
            isPresented: $showCoverFlow,
            title: "team_cover_title",
            aspectRatio: CoverImageSpec.shared.ASPECT_RATIO,
            maxEdge: Int(CoverImageSpec.shared.MAX_EDGE_PX),
            jpegQuality: Int(CoverImageSpec.shared.JPEG_QUALITY),
            canRemove: observer.coverUrl != nil,
            onImageReady: observer.uploadCover,
            onRemove: observer.removeCover
        )
        // A separate host, so the two flows' dialogs and covers never compete for one view.
        .background {
            Color.clear.imagePickerFlow(
                isPresented: $showAvatarFlow,
                title: "team_photo_title",
                aspectRatio: AvatarImageSpec.shared.ASPECT_RATIO,
                maxEdge: Int(AvatarImageSpec.shared.MAX_EDGE_PX),
                jpegQuality: Int(AvatarImageSpec.shared.JPEG_QUALITY),
                canRemove: observer.avatarUrl != nil,
                onImageReady: observer.uploadAvatar,
                onRemove: observer.removeAvatar
            )
        }
        .sheet(isPresented: $isEditing, onDismiss: observer.discard) {
            TeamEditView(
                name: Binding(get: { observer.name }, set: observer.setName),
                teamDescription: Binding(get: { observer.teamDescription }, set: observer.setDescription),
                isSaving: observer.isSaving,
                snackbar: $observer.snackbar,
                onSave: observer.save,
                onCancel: { isEditing = false }
            )
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.visible)
            .interactiveDismissDisabled(observer.isSaving)
        }
        .fullScreenCover(isPresented: $showInvite) {
            TeamInviteSheetView(teamName: observer.name, inviteCode: observer.inviteCode, onClose: { showInvite = false })
        }
        .onChange(of: observer.saveCompletedCount) { _, _ in isEditing = false }
        // Losing edit rights (e.g. role changed remotely) leaves edit mode.
        .onChange(of: observer.canEdit) { _, allowed in if !allowed { isEditing = false } }
        .task { observer.load(teamId: teamId, userId: userId) }
    }
}
