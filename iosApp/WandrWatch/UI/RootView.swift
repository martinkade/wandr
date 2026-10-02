import SwiftUI

/// Switches between picker, live workout and summary depending on the workout phase.
struct RootView: View {
    var manager: WorkoutManager

    var body: some View {
        switch manager.phase {
        case .idle:
            ActivityPickerView(manager: manager)
        case .running, .paused:
            LiveWorkoutContainer(manager: manager)
        case .summary(let workout):
            WorkoutSummaryView(workout: workout, onDone: manager.dismissSummary)
        }
    }
}

#Preview("Root") {
    RootView(manager: WorkoutManager())
}
