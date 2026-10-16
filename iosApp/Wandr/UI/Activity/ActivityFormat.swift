import Foundation
@preconcurrency import shared

/// Number and text formatting for the activity cards, details and the form (same rules as Android's `ActivityFormat`).
enum ActivityFormat {
    /// Below this a pace is meaningless (GPS noise on a standing recording).
    private static let minDistanceMeters = 50.0

    /// "51:07" below an hour, "1:05:07" from an hour on.
    static func duration(_ seconds: Double) -> String {
        let total = max(0, Int(seconds))
        let h = total / 3600, m = (total % 3600) / 60, s = total % 60
        return h > 0 ? String(format: "%d:%02d:%02d", h, m, s) : String(format: "%d:%02d", m, s)
    }

    /// Kilometers with two decimals below 100 km, one above, in the user's number format.
    static func distanceKm(_ meters: Double) -> String {
        let km = (meters / 1000).formatted(.number.precision(.fractionLength(meters >= 100_000 ? 1 : 2)))
        return "\(km) km"
    }

    /// "5:35 /km", or nil without a usable distance.
    static func pace(distanceMeters: Double, durationSeconds: Double) -> String? {
        guard distanceMeters >= minDistanceMeters, durationSeconds > 0 else { return nil }
        let perKm = Int((durationSeconds / (distanceMeters / 1000)).rounded())
        return String(format: "%d:%02d /km", perKm / 60, perKm % 60)
    }

    /// "17.9 km/h", or nil without a usable distance.
    static func speed(distanceMeters: Double, durationSeconds: Double) -> String? {
        guard distanceMeters >= minDistanceMeters, durationSeconds > 0 else { return nil }
        let kmh = distanceMeters / 1000 / (durationSeconds / 3600)
        return "\(kmh.formatted(.number.precision(.fractionLength(1)))) km/h"
    }

    static func elevation(_ meters: Double) -> String { "\(Int(meters)) m" }

    /// Localized name of an activity type; unknown types fall back to the capitalized value.
    static func typeName(_ type: String) -> String {
        let key = "activity_type_\(type)"
        let text = NSLocalizedString(key, comment: "")
        return text == key ? type.capitalized : text
    }

    static func typeSymbol(_ type: String) -> String {
        switch type {
        case "running": return "figure.run"
        case "cycling": return "figure.outdoor.cycle"
        default: return "figure.hiking"
        }
    }

    static func date(millis: Int64) -> Date { Date(timeIntervalSince1970: Double(millis) / 1000) }
}
