import SwiftUI

/// Edit screen shown in the profile's sheet: photo, name, bio; Cancel / Save in the sheet's toolbar.
struct ProfileEditView: View {
    @Binding var displayName: String
    @Binding var bio: String
    var avatarUrl: String? = nil
    var isSaving: Bool = false
    var isAvatarUpdating: Bool = false
    @Binding var snackbar: SnackbarMessage?
    var onSave: () -> Void = {}
    var onCancel: () -> Void = {}
    var onAvatarReady: (Data) -> Void = { _ in }
    var onRemoveAvatar: () -> Void = {}

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    AvatarEditorView(
                        displayName: displayName,
                        avatarUrl: avatarUrl,
                        isBusy: isAvatarUpdating,
                        onAvatarReady: onAvatarReady,
                        onRemoveAvatar: onRemoveAvatar
                    )
                    .frame(maxWidth: .infinity)
                    .listRowBackground(Color.clear)
                }

                Section(LocalizedStringKey("display_name_label")) {
                    TextField(LocalizedStringKey("display_name_label"), text: $displayName)
                        .textContentType(.name)
                }

                Section(LocalizedStringKey("bio_label")) {
                    TextField(LocalizedStringKey("bio_label"), text: $bio, axis: .vertical)
                        .lineLimit(3...8)
                }
            }
            .disabled(isSaving)
            .navigationTitle(LocalizedStringKey("profile_edit_title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(LocalizedStringKey("cancel_button"), action: onCancel)
                        .disabled(isSaving)
                }
                ToolbarItem(placement: .confirmationAction) {
                    if isSaving {
                        ProgressView()
                    } else {
                        Button(LocalizedStringKey("save_profile_button"), action: onSave)
                            .fontWeight(.semibold)
                    }
                }
            }
        }
        .snackbar($snackbar) // messages while the sheet is open (e.g. photo updated, errors)
    }
}

#Preview("Light Mode") {
    ProfileEditView(displayName: .constant("Martin Kade"), bio: .constant("Outdoor hiker & developer."), snackbar: .constant(nil))
        .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    ProfileEditView(displayName: .constant("Martin Kade"), bio: .constant(""), snackbar: .constant(nil))
        .preferredColorScheme(.dark)
}

#Preview("iPad, saving", traits: .fixedLayout(width: 700, height: 900)) {
    ProfileEditView(displayName: .constant("Martin Kade"), bio: .constant(""), isSaving: true,
                    snackbar: .constant(SnackbarMessage(text: "Save failed", isError: true)))
}
