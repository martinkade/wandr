import Foundation
import WatchConnectivity

/// Sends finished workouts to the iPhone via `transferUserInfo` (queued, guaranteed delivery).
/// Unsent workouts live in a [WorkoutOutbox] and are re-sent on launch; an entry is dropped after the transfer finished.
final class WatchConnectivityService: NSObject, WCSessionDelegate, @unchecked Sendable {
    static let shared = WatchConnectivityService()
    static let payloadKey = "workout"

    private let outbox: WorkoutOutbox

    init(outbox: WorkoutOutbox = .default) {
        self.outbox = outbox
        super.init()
    }

    /// Activates the session and re-queues everything that was not confirmed yet.
    func start() {
        guard WCSession.isSupported() else { return }
        let session = WCSession.default
        session.delegate = self
        session.activate()
    }

    /// Persists the workout in the outbox and queues it for transfer.
    func send(_ workout: WatchWorkout) {
        guard let json = try? workout.jsonString() else { return }
        outbox.add(id: workout.id, json: json)
        transfer(json)
    }

    private func transfer(_ json: String) {
        guard WCSession.isSupported(), WCSession.default.activationState == .activated else { return }
        WCSession.default.transferUserInfo([Self.payloadKey: json])
    }

    private func resendPending() {
        let alreadyQueued = Set(WCSession.default.outstandingUserInfoTransfers.compactMap {
            ($0.userInfo[Self.payloadKey] as? String).flatMap(WatchWorkout.decode)?.id
        })
        for (id, json) in outbox.pending() where !alreadyQueued.contains(id) {
            transfer(json)
        }
    }

    // MARK: WCSessionDelegate

    func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {
        if activationState == .activated { resendPending() }
    }

    func session(_ session: WCSession, didFinish userInfoTransfer: WCSessionUserInfoTransfer, error: Error?) {
        guard error == nil,
              let json = userInfoTransfer.userInfo[Self.payloadKey] as? String,
              let id = WatchWorkout.decode(json)?.id else { return }
        outbox.remove(id: id)
    }
}
