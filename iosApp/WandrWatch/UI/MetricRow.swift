import SwiftUI

/// A label + value line of the live workout screen.
struct MetricRow: View {
    var titleKey: LocalizedStringKey
    var value: String
    var unitKey: LocalizedStringKey?

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            Text(titleKey).font(.caption2).foregroundStyle(.secondary)
            Spacer()
            Text(value).font(.headline).monospacedDigit()
            if let unitKey { Text(unitKey).font(.caption2).foregroundStyle(.secondary) }
        }
    }
}

#Preview("Metric row") {
    MetricRow(titleKey: "metric_distance", value: "1.25", unitKey: "unit_km")
        .padding()
}

#Preview("Metric row dark") {
    MetricRow(titleKey: "metric_heart_rate", value: "132", unitKey: "unit_bpm")
        .padding()
        .preferredColorScheme(.dark)
}
