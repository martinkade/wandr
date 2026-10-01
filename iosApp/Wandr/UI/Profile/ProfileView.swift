import SwiftUI

/// Read-only profile. "Edit" opens `ProfileEditView` in a sheet; saving closes it.
/// Results (saved / photo updated / error) are reported in a snackbar (on the sheet while it is open).
struct ProfileView: View {
    @Binding var displayName: String
    @Binding var bio: String
    var avatarUrl: String? = nil
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
    /// Only for previews.
    var startEditing: Bool = false

    @State private var isEditing = false

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                AvatarPickerView(displayName: displayName, avatarUrl: avatarUrl, isBusy: isAvatarUpdating, isEnabled: false)

                VStack(spacing: 8) {
                    LabeledValueView(label: "display_name_label", value: displayName)
                    LabeledValueView(label: "bio_label", value: bio)
                }
            }
            .padding(24)
        }
        .background(
            LinearGradient(
                colors: [Color.wandrPrimary.opacity(0.08), Color.wandrSecondary.opacity(0.05)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()
        )
        .navigationTitle(LocalizedStringKey("user_profile_title"))
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button(LocalizedStringKey("logout_button"), role: .destructive, action: onLogout)
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button(LocalizedStringKey("profile_edit_button")) { isEditing = true }
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
        .onAppear { if startEditing { isEditing = true } }
    }
}

/// Connects `ProfileView` to the shared `ProfileViewModel` for a signed-in user.
struct ProfileContainerView: View {
    let userId: String?
    var onLogout: () -> Void = {}
    @StateObject private var observer = ProfileObserver()

    var body: some View {
        ProfileView(
            displayName: Binding(get: { observer.displayName }, set: observer.setDisplayName),
            bio: Binding(get: { observer.bio }, set: observer.setBio),
            avatarUrl: observer.avatarUrl,
            isSaving: observer.isSaving,
            isAvatarUpdating: observer.isAvatarUpdating,
            saveCompletedCount: observer.saveCompletedCount,
            snackbar: $observer.snackbar,
            onSave: observer.save,
            onCancel: observer.discard,
            onAvatarReady: observer.uploadAvatar,
            onRemoveAvatar: observer.removeAvatar,
            onLogout: onLogout
        )
        .task(id: userId) {
            if let userId { observer.load(userId: userId) }
        }
    }
}

#Preview("Read-only") {
    NavigationStack {
        ProfileView(displayName: .constant("Martin Kade"), bio: .constant("Outdoor hiker & developer."), snackbar: .constant(nil))
    }
}

#Preview("Read-only Dark") {
    NavigationStack {
        ProfileView(displayName: .constant("Martin Kade"), bio: .constant("Outdoor hiker & developer."), snackbar: .constant(nil))
    }
    .preferredColorScheme(.dark)
}

#Preview("With edit sheet") {
    NavigationStack {
        ProfileView(displayName: .constant("Martin Kade"), bio: .constant("Outdoor hiker & developer."),
                    snackbar: .constant(nil), startEditing: true)
    }
}

#Preview("iPad, snackbar", traits: .fixedLayout(width: 820, height: 1000)) {
    NavigationStack {
        ProfileView(displayName: .constant("Martin Kade"), bio: .constant(""),
                    snackbar: .constant(SnackbarMessage(text: "Profile saved")))
    }
}
