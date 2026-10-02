import Foundation
import HealthKit

/// The activity types a workout can be recorded for.
enum WorkoutKind: String, CaseIterable, Identifiable, Sendable {
    case hiking, running, cycling

    var id: String { rawValue }

    /// Value used in the wire format.
    var wireValue: String { rawValue }

    var healthKitType: HKWorkoutActivityType {
        switch self {
        case .hiking: .hiking
        case .running: .running
        case .cycling: .cycling
        }
    }

    var systemImage: String {
        switch self {
        case .hiking: "figure.hiking"
        case .running: "figure.run"
        case .cycling: "figure.outdoor.cycle"
        }
    }

    var titleKey: String { "activity_\(rawValue)" }
}
