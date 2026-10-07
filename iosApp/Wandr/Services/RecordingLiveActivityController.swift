import ActivityKit
import Foundation

/// Starts, updates and ends the Live Activity of the running recording (lock screen, Dynamic Island). The running time
/// counts up on its own (see `RecordingActivityAttributes`), so updates are only needed for the distance and for pausing.
@MainActor
final class RecordingLiveActivityController {
    static let shared = RecordingLiveActivityController()

    /// Only the id is kept: the activity itself is looked up where it is changed (it must not cross actors).
    private var activityId: String?

    func start(distanceMeters: Double, elapsedSeconds: Double) {
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return }
        end()
        let state = RecordingActivityAttributes.ContentState(
            distanceMeters: distanceMeters, isPaused: false,
            timerStart: Date().addingTimeInterval(-elapsedSeconds), elapsedSeconds: elapsedSeconds
        )
        activityId = (try? Activity.request(
            attributes: RecordingActivityAttributes(),
            content: ActivityContent(state: state, staleDate: nil)
        ))?.id
    }

    func update(distanceMeters: Double, elapsedSeconds: Double, isPaused: Bool) {
        guard let activityId else { return }
        let state = RecordingActivityAttributes.ContentState(
            distanceMeters: distanceMeters, isPaused: isPaused,
            timerStart: Date().addingTimeInterval(-elapsedSeconds), elapsedSeconds: elapsedSeconds
        )
        Task { await Self.apply(state, to: activityId) }
    }

    func end() {
        guard let activityId else { return }
        self.activityId = nil
        Task { await Self.finish(activityId) }
    }

    private nonisolated static func apply(_ state: RecordingActivityAttributes.ContentState, to id: String) async {
        let activity = Activity<RecordingActivityAttributes>.activities.first { $0.id == id }
        await activity?.update(ActivityContent(state: state, staleDate: nil))
    }

    private nonisolated static func finish(_ id: String) async {
        let activity = Activity<RecordingActivityAttributes>.activities.first { $0.id == id }
        await activity?.end(nil, dismissalPolicy: .immediate)
    }
}
