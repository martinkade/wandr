import SwiftUI
@preconcurrency import shared

/// The details of one activity (pushed from the feed or from a notification), with its own view model. The owner can
/// edit (form sheet) or delete it from the toolbar menu; a delete closes the screen.
struct ActivityDetailView: View {
    let activityId: String
    let userId: String
    var onDeleted: () -> Void = {}

    @StateObject private var observer = ActivityObserver()
    @Environment(\.dismiss) private var dismiss
    @State private var isEditing = false
    @State private var confirmDelete = false
    @State private var isHeaderCollapsed = false
    @State private var message: SnackbarMessage?

    private var state: ActivityState { observer.state }

    var body: some View {
        Group {
            if let activity = state.selectedActivity, activity.id == activityId {
                let author = state.authors[activity.userId]
                ActivityDetailContentView(
                    activity: activity,
                    authorName: author?.displayName ?? "",
                    authorAvatarUrl: author?.avatarUrl,
                    route: state.selectedTrack.isEmpty ? activity.route : state.selectedTrack,
                    isOwn: activity.userId == userId,
                    isHeaderCollapsed: $isHeaderCollapsed
                ) {
                    EntitySocialBar(type: .activity, entityId: activity.id, userId: userId, ownerId: activity.userId)
                }
                .navigationTitle(isHeaderCollapsed ? activity.title : "")
                .toolbar { ownerMenu(activity) }
                .activityFormSheet(
                    observer: observer, isPresented: $isEditing, message: $message,
                    userId: userId, teamId: activity.teamId,
                    onSuccess: { if $0 == .deleted { closeAfterDelete() } }
                )
                .confirmationDialog(
                    LocalizedStringKey("activity_delete_confirm_title"), isPresented: $confirmDelete, titleVisibility: .visible
                ) {
                    Button(LocalizedStringKey("social_delete"), role: .destructive) {
                        observer.send(ActivityIntentDeleteActivity(activityId: activity.id))
                    }
                    Button(LocalizedStringKey("cancel_button"), role: .cancel) {}
                } message: {
                    Text(LocalizedStringKey("activity_delete_confirm_message"))
                }
            } else {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color(.systemGroupedBackground))
                    .navigationTitle(LocalizedStringKey("activity_details_title"))
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .snackbar($message)
        .task(id: "\(activityId)|\(userId)") {
            // Loading the user's list establishes the signed-in user in the view model (owner check for edit / delete).
            observer.send(ActivityIntentLoadUserActivities(userId: userId))
            observer.send(ActivityIntentSelectActivity(activityId: activityId))
        }
    }

    @ToolbarContentBuilder
    private func ownerMenu(_ activity: Activity) -> some ToolbarContent {
        if state.canEdit {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Button {
                        observer.send(ActivityIntentStartEdit(activityId: activity.id))
                        isEditing = true
                    } label: {
                        Label(LocalizedStringKey("profile_edit_button"), systemImage: "pencil")
                    }
                    Button(role: .destructive) {
                        confirmDelete = true
                    } label: {
                        Label(LocalizedStringKey("activity_delete_button"), systemImage: "trash")
                    }
                } label: {
                    Image(systemName: "ellipsis")
                }
                .accessibilityLabel(Text(LocalizedStringKey("more_options")))
                .disabled(state.isSaving)
            }
        }
    }

    private func closeAfterDelete() {
        onDeleted()
        dismiss()
    }
}


// The container needs the shared view models (Koin); see `ActivityDetailContentView` for the content previews.
#Preview("Light Mode") { NavigationStack { ActivityDetailView(activityId: "a1", userId: "u1") }.preferredColorScheme(.light) }
#Preview("Dark Mode") { NavigationStack { ActivityDetailView(activityId: "a1", userId: "u1") }.preferredColorScheme(.dark) }
#Preview("Large Text") {
    NavigationStack { ActivityDetailView(activityId: "a1", userId: "u1") }.environment(\.dynamicTypeSize, .accessibility2)
}
