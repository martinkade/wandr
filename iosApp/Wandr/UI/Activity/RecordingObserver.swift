import SwiftUI
@preconcurrency import shared

/// The state of the running recording: the process-wide recording view model of the shared module (the same logic as on
/// Android: distance, time, pace, elevation, splits, saving with the conflict check).
@MainActor
final class RecordingObserver: ObservableObject {
    @Published private(set) var state: ActivityState

    private let viewModel = IosDependencies.shared.recordingViewModel()
    nonisolated(unsafe) private var job: (any Kotlinx_coroutines_coreJob)?

    init() {
        // The recording view model outlives the screen: a running recording is shown again as it is.
        state = viewModel.state.value as! ActivityState
    }

    func start() {
        job?.cancel(cause: nil)
        job = FlowObserverKt.watch(viewModel.state) { [weak self] value in
            guard let state = value as? ActivityState else { return }
            Task { @MainActor in self?.state = state }
        }
    }

    func send(_ intent: ActivityIntent) {
        viewModel.processIntent(intent: intent)
    }

    deinit { job?.cancel(cause: nil) }
}
