import SwiftUI
import shared

/// Target announced by a tapped push notification or notification list entry; the main view observes and consumes it.
struct PushTarget: Equatable {
    let entityType: String   // "activity" | "challenge"
    let entityId: String
}

@MainActor
final class PushNavigation: ObservableObject {
    static let shared = PushNavigation()
    @Published var target: PushTarget?

    func open(entityType: SocialEntityType, entityId: String) {
        target = PushTarget(entityType: entityType.wire, entityId: entityId)
    }

    func open(entityType: String, entityId: String) {
        target = PushTarget(entityType: entityType, entityId: entityId)
    }

    func consume() { target = nil }
}
