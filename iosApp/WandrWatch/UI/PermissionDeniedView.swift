import SwiftUI

/// Shown when Health or location access was denied. watchOS has no settings deep link, so the hint names the way.
struct PermissionDeniedView: View {
    var onClose: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 8) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .font(.title2)
                    .foregroundStyle(.orange)
                Text("permission_denied_title").font(.headline)
                Text("permission_denied_message").font(.footnote)
                Button("action_ok", action: onClose)
            }
        }
    }
}

#Preview("Denied 41mm") {
    PermissionDeniedView(onClose: {})
        .frame(width: 176, height: 215)
}

#Preview("Denied dark") {
    PermissionDeniedView(onClose: {})
        .frame(width: 205, height: 251)
        .preferredColorScheme(.dark)
}
