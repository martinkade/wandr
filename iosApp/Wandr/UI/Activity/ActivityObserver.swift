import SwiftUI
@preconcurrency import shared

/// The state of one activity screen (feed or details): its own `ActivityViewModel`, like one view model per screen on
/// Android, so the details opened from a notification do not change the list of the feed.
@MainActor
final class ActivityObserver: ObservableObject {
    @Published private(set) var state: ActivityState

    private let viewModel = IosDependencies.shared.activityViewModel()
    nonisolated(unsafe) private var job: (any Kotlinx_coroutines_coreJob)?

    init() {
        state = viewModel.state.value as! ActivityState
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

extension ActivitySuccess {
    /// The snackbar text of a finished save / delete.
    var message: String {
        switch self {
        case .created: return String(localized: "activity_created_message")
        case .updated: return String(localized: "activity_updated_message")
        case .deleted: return String(localized: "activity_deleted_message")
        case .merged: return String(localized: "activity_merged_message")
        case .trimmed: return String(localized: "activity_trimmed_message")
        case .discarded: return String(localized: "activity_discarded_message")
        default: return String(localized: "activity_created_message")
        }
    }
}
