import Foundation
import WatchConnectivity
@preconcurrency import shared

/// Receives workouts recorded on the Apple Watch (`transferUserInfo(["workout": json])`, queued and guaranteed by
/// WatchConnectivity, so nothing needs to be acknowledged) and hands them to the shared inbox. Importing them (and the
/// conflict wizard) is handled by `WatchImportObserver`.
final class WatchConnectivityReceiver: NSObject, WCSessionDelegate, @unchecked Sendable {
    static let instance = WatchConnectivityReceiver()

    private let inbox = IosDependencies.shared.watchWorkoutInbox()

    /// Call once after Koin is initialised.
    func activate() {
        guard WCSession.isSupported() else { return }
        WCSession.default.delegate = self
        WCSession.default.activate()
    }

    func session(_ session: WCSession, didReceiveUserInfo userInfo: [String: Any] = [:]) {
        guard let json = userInfo["workout"] as? String else { return }
        _ = inbox.offerJson(json: json)
    }

    func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {}
    func sessionDidBecomeInactive(_ session: WCSession) {}
    func sessionDidDeactivate(_ session: WCSession) { session.activate() }
}
