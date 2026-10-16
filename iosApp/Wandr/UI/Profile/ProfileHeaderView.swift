import SwiftUI

/// Avatar (tap to change it right here), name and bio at the top of the profile.
struct ProfileHeaderView: View {
    let displayName: String
    var bio: String = ""
    var avatarUrl: String? = nil
    var isAvatarUpdating: Bool = false
    var onAvatarReady: (Data) -> Void = { _ in }
    var onRemoveAvatar: () -> Void = {}

    var body: some View {
        VStack(spacing: 12) {
            AvatarEditorView(
                displayName: displayName,
                avatarUrl: avatarUrl,
                isBusy: isAvatarUpdating,
                onAvatarReady: onAvatarReady,
                onRemoveAvatar: onRemoveAvatar
            )

            VStack(spacing: 4) {
                Text(displayName.isEmpty ? String(localized: "empty_value_placeholder") : displayName)
                    .font(.title2.weight(.bold))
                    .multilineTextAlignment(.center)
                if !bio.isEmpty {
                    Text(bio)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                        .lineLimit(3)
                }
            }
        }
        .frame(maxWidth: .infinity)
    }
}

#Preview("Light Mode") {
    ProfileHeaderView(displayName: "Martin Kade", bio: "Outdoor hiker & developer.")
        .padding()
        .background(Color(.systemGroupedBackground))
        .preferredColorScheme(.light)
}

#Preview("Dark Mode, busy") {
    ProfileHeaderView(displayName: "Martin Kade", isAvatarUpdating: true)
        .padding()
        .background(Color(.systemGroupedBackground))
        .preferredColorScheme(.dark)
}

#Preview("Large text") {
    ProfileHeaderView(displayName: "Martin Kade", bio: "Outdoor hiker & developer.")
        .padding()
        .dynamicTypeSize(.accessibility2)
}
