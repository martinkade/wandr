@preconcurrency import shared
import SwiftUI

/// Round avatar with its whole edit flow (1:1 crop, `AvatarImageSpec`). Used for users and teams.
struct AvatarEditorView: View {
    let displayName: String
    var avatarUrl: String? = nil
    var isBusy: Bool = false
    var isEnabled: Bool = true
    var title: LocalizedStringKey = "avatar_change_title"
    var onAvatarReady: (Data) -> Void = { _ in }
    var onRemoveAvatar: () -> Void = {}

    @State private var showSources = false

    var body: some View {
        AvatarPickerView(displayName: displayName, avatarUrl: avatarUrl, isBusy: isBusy, isEnabled: isEnabled) {
            showSources = true
        }
        .imagePickerFlow(
            isPresented: $showSources,
            title: title,
            aspectRatio: AvatarImageSpec.shared.ASPECT_RATIO,
            maxEdge: Int(AvatarImageSpec.shared.MAX_EDGE_PX),
            jpegQuality: Int(AvatarImageSpec.shared.JPEG_QUALITY),
            canRemove: avatarUrl != nil,
            onImageReady: onAvatarReady,
            onRemove: onRemoveAvatar
        )
    }
}

#Preview("With initials") { AvatarEditorView(displayName: "Martin Kade") }
#Preview("Dark, busy") { AvatarEditorView(displayName: "Martin Kade", isBusy: true).preferredColorScheme(.dark) }
