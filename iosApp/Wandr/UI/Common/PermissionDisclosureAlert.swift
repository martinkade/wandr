import SwiftUI

extension View {
    /// Explains why a permission is needed. Present it BEFORE triggering the system permission prompt.
    func permissionDisclosure(
        isPresented: Binding<Bool>,
        title: LocalizedStringKey,
        message: LocalizedStringKey,
        onContinue: @escaping () -> Void
    ) -> some View {
        alert(title, isPresented: isPresented) {
            Button(LocalizedStringKey("permission_continue"), action: onContinue)
            Button(LocalizedStringKey("permission_not_now"), role: .cancel) {}
        } message: {
            Text(message)
        }
    }
}

#Preview("Light Mode") {
    Color.clear
        .permissionDisclosure(isPresented: .constant(true), title: "permission_location_title", message: "permission_location_message") {}
        .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    Color.clear
        .permissionDisclosure(isPresented: .constant(true), title: "permission_location_title", message: "permission_location_message") {}
        .preferredColorScheme(.dark)
}
