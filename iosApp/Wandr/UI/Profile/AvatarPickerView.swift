import SwiftUI

/// A round avatar that can be tapped to change it (a small glass camera badge shows that it can).
struct AvatarPickerView: View {
    let displayName: String
    var avatarUrl: String? = nil
    var isBusy: Bool = false
    var isEnabled: Bool = true
    var size: CGFloat = 96
    var onPick: () -> Void = {}

    var body: some View {
        Button {
            if isEnabled && !isBusy { onPick() }
        } label: {
            ZStack {
                AvatarView(url: avatarUrl, name: displayName, size: size)
                if isBusy {
                    Circle().fill(Color.black.opacity(0.4)).frame(width: size, height: size)
                    ProgressView().tint(.white)
                }
            }
            .overlay(alignment: .bottomTrailing) {
                if isEnabled && !isBusy {
                    Image(systemName: "camera.fill")
                        .font(.system(size: size * 0.14, weight: .semibold))
                        .foregroundStyle(.primary)
                        .frame(width: size * 0.32, height: size * 0.32)
                        .wandrGlassCircle(interactive: false)
                }
            }
        }
        .buttonStyle(.plain)
        .disabled(!isEnabled)
        .accessibilityLabel(Text(LocalizedStringKey("avatar_content_description")))
    }
}

#Preview("Light Mode") {
    AvatarPickerView(displayName: "Martin Kade").padding().preferredColorScheme(.light)
}

#Preview("Dark Mode, busy") {
    AvatarPickerView(displayName: "Martin Kade", isBusy: true).padding().preferredColorScheme(.dark)
}

#Preview("Small, read-only") {
    AvatarPickerView(displayName: "Martin Kade", isEnabled: false, size: 56).padding().dynamicTypeSize(.accessibility2)
}
