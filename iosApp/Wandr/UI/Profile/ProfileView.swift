import SwiftUI

struct ProfileView: View {
    @Binding var displayName: String
    @Binding var bio: String
    var avatarUrl: String? = nil
    var isSaving: Bool = false
    var isAvatarUpdating: Bool = false
    var errorMessage: String? = nil
    var successMessage: String? = nil
    var onSave: () -> Void = {}
    var onAvatarReady: (Data) -> Void = { _ in }
    var onRemoveAvatar: () -> Void = {}

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                Text(LocalizedStringKey("user_profile_title"))
                    .font(.system(size: 28, weight: .bold, design: .rounded))
                    .foregroundColor(.primary)

                AvatarEditorView(
                    displayName: displayName,
                    avatarUrl: avatarUrl,
                    isBusy: isAvatarUpdating,
                    onAvatarReady: onAvatarReady,
                    onRemoveAvatar: onRemoveAvatar
                )

                VStack(spacing: 16) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(LocalizedStringKey("display_name_label"))
                            .font(.caption)
                            .foregroundColor(.secondary)

                        TextField(LocalizedStringKey("display_name_label"), text: $displayName)
                            .textFieldStyle(.plain)
                            .padding()
                            .background(.ultraThinMaterial)
                            .cornerRadius(12)
                    }

                    VStack(alignment: .leading, spacing: 6) {
                        Text(LocalizedStringKey("bio_label"))
                            .font(.caption)
                            .foregroundColor(.secondary)

                        TextEditor(text: $bio)
                            .frame(height: 100)
                            .padding(8)
                            .scrollContentBackground(.hidden)
                            .background(.ultraThinMaterial)
                            .cornerRadius(12)
                    }
                }

                if let errorMessage {
                    Text(errorMessage).font(.footnote).foregroundColor(.red)
                }
                if let successMessage {
                    Text(successMessage).font(.footnote).foregroundColor(.green)
                }

                Button(action: onSave) {
                    Group {
                        if isSaving {
                            ProgressView().tint(.white)
                        } else {
                            Text(LocalizedStringKey("save_profile_button")).font(.headline)
                        }
                    }
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(Color.blue)
                    .cornerRadius(12)
                }
                .disabled(isSaving)
            }
            .padding(24)
        }
        .background(
            LinearGradient(
                colors: [Color.blue.opacity(0.08), Color.purple.opacity(0.05)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()
        )
    }
}

/// Connects `ProfileView` to the shared `ProfileViewModel` for a signed-in user.
struct ProfileContainerView: View {
    let userId: String?
    @StateObject private var observer = ProfileObserver()

    var body: some View {
        ProfileView(
            displayName: Binding(get: { observer.displayName }, set: observer.setDisplayName),
            bio: Binding(get: { observer.bio }, set: observer.setBio),
            avatarUrl: observer.avatarUrl,
            isSaving: observer.isSaving,
            isAvatarUpdating: observer.isAvatarUpdating,
            errorMessage: observer.errorMessage,
            successMessage: observer.successMessage,
            onSave: observer.save,
            onAvatarReady: observer.uploadAvatar,
            onRemoveAvatar: observer.removeAvatar
        )
        .task(id: userId) {
            if let userId { observer.load(userId: userId) }
        }
    }
}

#Preview("Light Mode") {
    ProfileView(displayName: .constant("Martin Kade"), bio: .constant("Outdoor hiker & developer."))
        .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    ProfileView(displayName: .constant("Martin Kade"), bio: .constant("Outdoor hiker & developer."), successMessage: "Profile saved!")
        .preferredColorScheme(.dark)
}

#Preview("iPad", traits: .fixedLayout(width: 820, height: 1000)) {
    ProfileView(displayName: .constant("Martin Kade"), bio: .constant(""), errorMessage: "Save failed")
}
