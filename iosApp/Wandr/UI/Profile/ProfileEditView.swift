import SwiftUI

/// Edit screen shown in the profile's sheet: photo, name, bio and Cancel / Save.
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
        ScrollView {
            VStack(spacing: 20) {
                Text(LocalizedStringKey("profile_edit_title"))
                    .font(.system(size: 22, weight: .bold, design: .rounded))
                    .foregroundColor(.wandrAccentText)
                    .padding(.top, 8)

                AvatarEditorView(
                    displayName: displayName,
                    avatarUrl: avatarUrl,
                    isBusy: isAvatarUpdating,
                    onAvatarReady: onAvatarReady,
                    onRemoveAvatar: onRemoveAvatar
                )

                VStack(alignment: .leading, spacing: 6) {
                    Text(LocalizedStringKey("display_name_label")).font(.caption).foregroundColor(.secondary)
                    TextField(LocalizedStringKey("display_name_label"), text: $displayName)
                        .textFieldStyle(.plain)
                        .padding()
                        .background(.ultraThinMaterial)
                        .cornerRadius(12)
                }

                VStack(alignment: .leading, spacing: 6) {
                    Text(LocalizedStringKey("bio_label")).font(.caption).foregroundColor(.secondary)
                    TextEditor(text: $bio)
                        .frame(height: 100)
                        .padding(8)
                        .scrollContentBackground(.hidden)
                        .background(.ultraThinMaterial)
                        .cornerRadius(12)
                }

                HStack(spacing: 12) {
                    Button(action: onCancel) {
                        Text(LocalizedStringKey("cancel_button"))
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .frame(height: 50)
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.secondary.opacity(0.5)))
                    }
                    .disabled(isSaving)

                    Button(action: onSave) {
                        Group {
                            if isSaving {
                                ProgressView().tint(.wandrOnPrimary)
                            } else {
                                Text(LocalizedStringKey("save_profile_button")).font(.headline)
                            }
                        }
                        .foregroundColor(.wandrOnPrimary)
                        .frame(maxWidth: .infinity)
                        .frame(height: 50)
                        .background(Color.wandrPrimary)
                        .cornerRadius(12)
                    }
                    .disabled(isSaving)
                }
            }
            .padding(24)
        }
        .background(Color(.systemBackground).ignoresSafeArea())
        .snackbar($snackbar) // messages while the sheet is open (e.g. photo updated, errors)
    }
}

#Preview("Light Mode") {
    ProfileEditView(displayName: .constant("Martin Kade"), bio: .constant("Outdoor hiker & developer."), snackbar: .constant(nil))
}

#Preview("Dark Mode") {
    ProfileEditView(displayName: .constant("Martin Kade"), bio: .constant(""), snackbar: .constant(nil))
        .preferredColorScheme(.dark)
}

#Preview("iPad, saving", traits: .fixedLayout(width: 700, height: 900)) {
    ProfileEditView(displayName: .constant("Martin Kade"), bio: .constant(""), isSaving: true,
                    snackbar: .constant(SnackbarMessage(text: "Save failed", isError: true)))
}
