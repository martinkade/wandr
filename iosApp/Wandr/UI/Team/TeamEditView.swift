@preconcurrency import shared
import SwiftUI

/// Edit screen shown in the group's sheet: cover (4:3), avatar (1:1), name, description, Cancel / Save.
struct TeamEditView: View {
    @Binding var name: String
    @Binding var teamDescription: String
    var avatarUrl: String? = nil
    var coverUrl: String? = nil
    var isSaving: Bool = false
    var isImageUpdating: Bool = false
    @Binding var snackbar: SnackbarMessage?
    var onSave: () -> Void = {}
    var onCancel: () -> Void = {}
    var onAvatarReady: (Data) -> Void = { _ in }
    var onRemoveAvatar: () -> Void = {}
    var onCoverReady: (Data) -> Void = { _ in }
    var onRemoveCover: () -> Void = {}

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                Text(LocalizedStringKey("team_edit_title"))
                    .font(.system(size: 22, weight: .bold, design: .rounded))
                    .foregroundColor(.wandrAccentText)
                    .padding(.top, 8)

                TeamCoverView(
                    coverUrl: coverUrl,
                    isEditing: true,
                    isBusy: isImageUpdating,
                    onImageReady: onCoverReady,
                    onRemove: onRemoveCover
                )

                AvatarEditorView(
                    displayName: name,
                    avatarUrl: avatarUrl,
                    isBusy: isImageUpdating,
                    title: "team_photo_title",
                    onAvatarReady: onAvatarReady,
                    onRemoveAvatar: onRemoveAvatar
                )

                VStack(alignment: .leading, spacing: 6) {
                    Text(LocalizedStringKey("team_name_label")).font(.caption).foregroundColor(.secondary)
                    TextField(LocalizedStringKey("team_name_label"), text: $name)
                        .textFieldStyle(.plain)
                        .padding()
                        .background(.ultraThinMaterial)
                        .cornerRadius(12)
                }

                VStack(alignment: .leading, spacing: 6) {
                    Text(LocalizedStringKey("team_description_label")).font(.caption).foregroundColor(.secondary)
                    TextEditor(text: $teamDescription)
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
        .snackbar($snackbar) // messages while the sheet is open (e.g. image updated, errors)
    }
}

#Preview("Light Mode") {
    TeamEditView(name: .constant("Alpine Trail Blazers"), teamDescription: .constant("Hiking group for weekend trips."),
                 snackbar: .constant(nil))
}

#Preview("Dark Mode") {
    TeamEditView(name: .constant("Alpine Trail Blazers"), teamDescription: .constant(""), snackbar: .constant(nil))
        .preferredColorScheme(.dark)
}

#Preview("iPad, saving", traits: .fixedLayout(width: 700, height: 1000)) {
    TeamEditView(name: .constant("Alpine Trail Blazers"), teamDescription: .constant(""), isSaving: true,
                 snackbar: .constant(SnackbarMessage(text: "Save failed", isError: true)))
}
