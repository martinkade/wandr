import Combine
import Foundation
@preconcurrency import shared

/// Runs a recording independently of its screen (one per app, like the recording service on Android): it feeds the shared
/// recording view model with the GPS fixes and the clock, reacts to the pause / resume button of the Live Activity and keeps
/// that Live Activity up to date. The screen can be closed and opened again while a recording runs.
@MainActor
final class RecordingCoordinator {
    static let shared = RecordingCoordinator()

    let location = LocationTrackingManager.shared
    private let viewModel = IosDependencies.shared.recordingViewModel()
    private var cancellables = Set<AnyCancellable>()
    private var timer: Timer?
    private var lastLiveActivityUpdate: Date = .distantPast

    private var state: ActivityState { viewModel.state.value as! ActivityState }
    var isRecording: Bool { state.isTracking }

    private init() {
        // Every fix updates the GPS status; the points are only recorded while a recording runs (the view model ignores
        // them while it is paused).
        location.$currentTrackpoint
            .compactMap { $0 }
            .sink { [weak self] point in self?.onFix(point) }
            .store(in: &cancellables)
        // The pause / resume button of the Live Activity (lock screen, Dynamic Island).
        NotificationCenter.default.publisher(for: .recordingCommand)
            .sink { [weak self] note in
                guard let self, self.isRecording else { return }
                switch (note.userInfo?["command"] as? String).flatMap(RecordingCommand.init(rawValue:)) {
                case .pause: self.pause()
                case .resume: self.resume()
                case nil: break
                }
            }
            .store(in: &cancellables)
    }

    func start(activityType: String) {
        viewModel.processIntent(intent: ActivityIntentStartGpsTracking(activityType: activityType))
        location.startTracking()
        RecordingLiveActivityController.shared.start(distanceMeters: 0, elapsedSeconds: 0)
        // The clock counts seconds even when no fix arrives.
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] _ in
            Task { @MainActor in self?.tick() }
        }
    }

    func pause() {
        viewModel.processIntent(intent: ActivityIntentPauseGpsTracking.shared)
        pushLiveActivity(force: true)
    }

    func resume() {
        viewModel.processIntent(intent: ActivityIntentResumeGpsTracking.shared)
        pushLiveActivity(force: true)
    }

    func discard() {
        viewModel.processIntent(intent: ActivityIntentDiscardRecording.shared)
        endSession()
    }

    func save(userId: String, teamId: String?, title: String) {
        // The route's map tiles are loaded now, so the saved activity shows its map without network later.
        OsmTileCache.prefetch(route: state.liveTrackpoints)
        viewModel.processIntent(intent: ActivityIntentStopAndSaveGpsTracking(userId: userId, teamId: teamId, title: title))
    }

    /// GPS, clock and Live Activity stop once the recording is over (saved, merged, trimmed or discarded).
    func endSession() {
        timer?.invalidate()
        timer = nil
        location.stopTracking()
        RecordingLiveActivityController.shared.end()
    }

    private func onFix(_ point: GpsTrackpoint) {
        viewModel.processIntent(intent: ActivityIntentGpsFixChanged(accuracyMeters: location.horizontalAccuracy.map { KotlinFloat(float: $0) }))
        guard isRecording else { return }
        viewModel.processIntent(intent: ActivityIntentAddTrackpoint(trackpoint: point))
        pushLiveActivity(force: false)
    }

    private func tick() {
        guard isRecording else { return }
        viewModel.processIntent(intent: ActivityIntentTick(nowMillis: Int64(Date().timeIntervalSince1970 * 1000)))
    }

    /// The Live Activity counts the time itself; it needs the distance (at most every 5 s) and every pause / resume.
    private func pushLiveActivity(force: Bool) {
        guard force || Date().timeIntervalSince(lastLiveActivityUpdate) >= 5 else { return }
        lastLiveActivityUpdate = Date()
        let current = state
        RecordingLiveActivityController.shared.update(
            distanceMeters: current.liveDistanceMeters, elapsedSeconds: current.liveDurationSeconds, isPaused: current.isPaused
        )
    }
}
