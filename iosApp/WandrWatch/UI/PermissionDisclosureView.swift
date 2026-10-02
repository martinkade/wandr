import SwiftUI

/// Explains why WANDR needs Health and location access. Must be shown before the system prompts.
struct PermissionDisclosureView: View {
    var onContinue: () -> Void
    var onNotNow: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 8) {
                Image(systemName: "heart.text.square.fill")
                    .font(.title2)
                    .foregroundStyle(.tint)
                Text("permission_title").font(.headline)
                Text("permission_message").font(.footnote)
                Button("permission_continue", action: onContinue)
                    .buttonStyle(.borderedProminent)
                Button("permission_not_now", action: onNotNow)
            }
        }
    }
}

#Preview("Disclosure 41mm", traits: .sizeThatFitsLayout) {
    PermissionDisclosureView(onContinue: {}, onNotNow: {})
        .frame(width: 176, height: 215)
}

#Preview("Disclosure 49mm dark") {
    PermissionDisclosureView(onContinue: {}, onNotNow: {})
        .frame(width: 205, height: 251)
        .preferredColorScheme(.dark)
}
