import ActivityKit
import AppIntents
import SwiftUI
import WidgetKit

/// The recording on the lock screen and in the Dynamic Island: running time, distance and a pause / resume button.
struct RecordingLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: RecordingActivityAttributes.self) { context in
            RecordingLockScreenView(state: context.state)
                .padding(16)
                .activityBackgroundTint(Color(.systemBackground).opacity(0.85))
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    RecordingTimeText(state: context.state)
                        .font(.title2.weight(.bold))
                        .monospacedDigit()
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Text(recordingDistanceText(context.state.distanceMeters))
                        .font(.title2.weight(.bold))
                        .monospacedDigit()
                }
                DynamicIslandExpandedRegion(.bottom) {
                    RecordingToggleButton(isPaused: context.state.isPaused)
                }
            } compactLeading: {
                Image(systemName: context.state.isPaused ? "pause.fill" : "figure.hiking")
            } compactTrailing: {
                RecordingTimeText(state: context.state)
                    .monospacedDigit()
                    .frame(maxWidth: 56)
            } minimal: {
                Image(systemName: context.state.isPaused ? "pause.fill" : "figure.hiking")
            }
        }
    }
}

/// The running time: counts up on its own while recording, shows the frozen time while paused.
struct RecordingTimeText: View {
    let state: RecordingActivityAttributes.ContentState

    var body: some View {
        if state.isPaused {
            Text(recordingClockText(state.elapsedSeconds))
        } else {
            Text(state.timerStart, style: .timer)
        }
    }
}

func recordingClockText(_ seconds: Double) -> String {
    let total = Int(seconds)
    let hours = total / 3600
    let minutes = (total % 3600) / 60
    let secs = total % 60
    return hours > 0 ? String(format: "%d:%02d:%02d", hours, minutes, secs) : String(format: "%02d:%02d", minutes, secs)
}

func recordingDistanceText(_ meters: Double) -> String {
    String(format: "%.2f km", meters / 1000.0)
}

/// Pause while recording, resume while paused; the intent runs in the app without opening it.
struct RecordingToggleButton: View {
    let isPaused: Bool

    var body: some View {
        if isPaused {
            Button(intent: ResumeRecordingIntent()) {
                Label("resume_button", systemImage: "play.fill").frame(maxWidth: .infinity)
            }
        } else {
            Button(intent: PauseRecordingIntent()) {
                Label("pause_button", systemImage: "pause.fill").frame(maxWidth: .infinity)
            }
        }
    }
}
