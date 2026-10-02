import Foundation

/// Pure formatting helpers for the workout metrics (unit labels come from the localized strings).
enum MetricFormatter {
    static func duration(_ seconds: TimeInterval) -> String {
        let total = max(0, Int(seconds))
        let h = total / 3600, m = (total % 3600) / 60, s = total % 60
        return h > 0 ? String(format: "%d:%02d:%02d", h, m, s) : String(format: "%02d:%02d", m, s)
    }

    /// Kilometers with two decimals, e.g. `1.25`.
    static func kilometers(_ meters: Double) -> String {
        String(format: "%.2f", meters / 1000)
    }

    static func meters(_ meters: Double) -> String {
        String(format: "%.0f", meters)
    }
}
