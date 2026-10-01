import SwiftUI

struct SnackbarMessage: Identifiable, Equatable {
    let id = UUID()
    let text: String
    var isError = false
}

/// Transient message pill, like an Android snackbar.
struct SnackbarView: View {
    let message: SnackbarMessage

    var body: some View {
        Text(message.text)
            .font(.subheadline)
            .foregroundColor(message.isError ? .white : Color(.systemBackground))
            .multilineTextAlignment(.leading)
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(message.isError ? Color.red : Color(.label))
            .cornerRadius(12)
            .shadow(radius: 4)
            .padding(16)
            .accessibilityElement(children: .combine)
    }
}

extension View {
    /// Shows `message` at the bottom and clears it after `duration` seconds.
    func snackbar(_ message: Binding<SnackbarMessage?>, duration: Double = 3) -> some View {
        overlay(alignment: .bottom) {
            if let current = message.wrappedValue {
                SnackbarView(message: current)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .task(id: current.id) {
                        try? await Task.sleep(for: .seconds(duration))
                        if message.wrappedValue?.id == current.id { message.wrappedValue = nil }
                    }
            }
        }
        .animation(.default, value: message.wrappedValue?.id)
    }
}

#Preview("Light Mode") {
    SnackbarView(message: SnackbarMessage(text: "Profile saved")).preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    SnackbarView(message: SnackbarMessage(text: "Profile saved")).preferredColorScheme(.dark)
}

#Preview("Error") {
    SnackbarView(message: SnackbarMessage(text: "Save failed: no connection", isError: true))
}
