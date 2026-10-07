import SwiftUI
import WidgetKit

/// The lock screen / banner layout of the recording Live Activity.
struct RecordingLockScreenView: View {
    let state: RecordingActivityAttributes.ContentState

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Label(
                    state.isPaused ? "recording_notification_paused" : "recording_notification_title",
                    systemImage: state.isPaused ? "pause.circle.fill" : "record.circle"
                )
                .font(.subheadline.weight(.semibold))
                Spacer()
            }
            HStack(alignment: .firstTextBaseline) {
                RecordingTimeText(state: state)
                    .font(.system(size: 36, weight: .bold))
                    .monospacedDigit()
                Spacer()
                Text(recordingDistanceText(state.distanceMeters))
                    .font(.system(size: 28, weight: .semibold))
                    .monospacedDigit()
            }
            RecordingToggleButton(isPaused: state.isPaused)
                .buttonStyle(.bordered)
        }
    }
}
