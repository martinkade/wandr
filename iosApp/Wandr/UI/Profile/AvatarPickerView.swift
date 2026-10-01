import SwiftUI

struct AvatarPickerView: View {
    let displayName: String
    var avatarUrl: String? = nil
    var isBusy: Bool = false
    var isEnabled: Bool = true
    var onPick: () -> Void = {}

    var body: some View {
        let initials = String(displayName.prefix(2)).uppercased()

        ZStack {
            Circle()
                .fill(.ultraThinMaterial)
                .frame(width: 96, height: 96)
                .shadow(radius: 4)

            if let avatarUrl, let url = URL(string: avatarUrl) {
                AsyncImage(url: url) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    ProgressView()
                }
                .frame(width: 96, height: 96)
                .clipShape(Circle())
                .accessibilityLabel(Text(LocalizedStringKey("avatar_content_description")))
            } else {
                Text(initials.isEmpty ? "WA" : initials)
                    .font(.system(size: 32, weight: .bold, design: .rounded))
                    .foregroundColor(.wandrAccentText)
            }

            if isBusy {
                Circle().fill(Color.black.opacity(0.4)).frame(width: 96, height: 96)
                ProgressView().tint(.white)
            }
        }
        .onTapGesture {
            if isEnabled && !isBusy { onPick() }
        }
    }
}

#Preview("Light Mode") {
    AvatarPickerView(displayName: "Martin Kade")
        .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    AvatarPickerView(displayName: "Martin Kade")
        .preferredColorScheme(.dark)
}

#Preview("Busy") {
    AvatarPickerView(displayName: "Martin Kade", isBusy: true)
}
