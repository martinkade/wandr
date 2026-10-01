import SwiftUI

struct AvatarPickerView: View {
    let displayName: String
    var onPick: () -> Void = {}

    var body: some View {
        let initials = String(displayName.prefix(2)).uppercased()

        ZStack {
            Circle()
                .fill(.ultraThinMaterial)
                .frame(width: 96, height: 96)
                .shadow(radius: 4)

            Text(initials.isEmpty ? "WA" : initials)
                .font(.system(size: 32, weight: .bold, design: .rounded))
                .foregroundColor(.blue)
        }
        .onTapGesture {
            onPick()
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
