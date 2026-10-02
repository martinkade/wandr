import CoreLocation
import HealthKit

/// Whether the workout permissions still need the disclosure + system prompt.
enum PermissionState: Equatable, Sendable {
    case granted
    case needsDisclosure
    case denied

    static func current(healthStore: HKHealthStore = HKHealthStore(), location: CLAuthorizationStatus) -> PermissionState {
        let health = healthStore.authorizationStatus(for: HKObjectType.workoutType())
        switch (health, location) {
        case (.sharingDenied, _), (_, .denied), (_, .restricted):
            return .denied
        case (.sharingAuthorized, .authorizedAlways), (.sharingAuthorized, .authorizedWhenInUse):
            return .granted
        default:
            return .needsDisclosure
        }
    }
}
