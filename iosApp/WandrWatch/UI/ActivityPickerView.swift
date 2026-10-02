import SwiftUI

/// Type picker (hiking, running, cycling). Shows the permission disclosure before the first system prompt.
struct ActivityPickerView: View {
    var manager: WorkoutManager
    @State private var showDisclosure = false

    var body: some View {
        NavigationStack {
            List(WorkoutKind.allCases) { kind in
                Button {
                    manager.requestStart(kind)
                    switch manager.permission {
                    case .needsDisclosure, .denied: showDisclosure = true
                    case .granted: break
                    }
                } label: {
                    Label(LocalizedStringKey(kind.titleKey), systemImage: kind.systemImage)
                }
            }
            .navigationTitle("app_name")
            .sheet(isPresented: $showDisclosure, onDismiss: manager.cancelPending) {
                PermissionFlowView(manager: manager, isPresented: $showDisclosure)
            }
        }
    }
}

#Preview("Picker") {
    ActivityPickerView(manager: WorkoutManager())
}

#Preview("Picker dark") {
    ActivityPickerView(manager: WorkoutManager())
        .preferredColorScheme(.dark)
}
