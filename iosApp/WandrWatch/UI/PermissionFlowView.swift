import SwiftUI

/// Shows the disclosure (or the denied hint) and only then triggers the system permission prompts.
struct PermissionFlowView: View {
    var manager: WorkoutManager
    @Binding var isPresented: Bool

    var body: some View {
        switch manager.permission {
        case .denied:
            PermissionDeniedView(onClose: { isPresented = false })
        default:
            PermissionDisclosureView(
                onContinue: {
                    Task {
                        await manager.requestAuthorizationAfterDisclosure()
                        isPresented = false
                    }
                },
                onNotNow: { isPresented = false }
            )
        }
    }
}

#Preview("Flow") {
    PermissionFlowView(manager: WorkoutManager(), isPresented: .constant(true))
}
