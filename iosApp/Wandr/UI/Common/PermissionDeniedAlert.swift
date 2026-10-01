import SwiftUI

extension View {
    /// Shown when the system will not prompt again; offers a shortcut to the app's settings.
    func permissionDeniedAlert(isPresented: Binding<Bool>, message: LocalizedStringKey) -> some View {
        alert(LocalizedStringKey("permission_denied_title"), isPresented: isPresented) {
            Button(LocalizedStringKey("permission_open_settings")) {
                if let url = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(url)
                }
            }
            Button(LocalizedStringKey("cancel_button"), role: .cancel) {}
        } message: {
            Text(message)
        }
    }
}

#Preview("Light Mode") {
    Color.clear
        .permissionDeniedAlert(isPresented: .constant(true), message: "permission_camera_denied")
        .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    Color.clear
        .permissionDeniedAlert(isPresented: .constant(true), message: "permission_camera_denied")
        .preferredColorScheme(.dark)
}
