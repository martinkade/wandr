import SwiftUI

/// Adapts the [WorkoutManager] to the stateless [LiveWorkoutView].
struct LiveWorkoutContainer: View {
    var manager: WorkoutManager

    var body: some View {
        LiveWorkoutView(
            kind: manager.kind,
            metrics: manager.metrics,
            isPaused: manager.phase == .paused,
            elapsed: { manager.elapsed(at: $0) },
            onPause: manager.pause,
            onResume: manager.resume,
            onStop: manager.stop
        )
    }
}

#Preview("Container") {
    LiveWorkoutContainer(manager: WorkoutManager())
}
