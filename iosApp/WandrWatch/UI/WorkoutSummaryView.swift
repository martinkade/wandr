import SwiftUI

/// Result of a finished workout; it is already queued for the iPhone.
struct WorkoutSummaryView: View {
    var workout: WatchWorkout
    var onDone: () -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 6) {
                Text("summary_title").font(.headline)
                MetricRow(titleKey: "metric_time",
                          value: MetricFormatter.duration(Double(workout.endTime - workout.startTime) / 1000), unitKey: nil)
                MetricRow(titleKey: "metric_distance", value: MetricFormatter.kilometers(workout.distanceMeters), unitKey: "unit_km")
                MetricRow(titleKey: "metric_elevation", value: MetricFormatter.meters(workout.elevationGainMeters), unitKey: "unit_m")
                MetricRow(titleKey: "metric_avg_heart_rate",
                          value: workout.averageHeartRate.map(String.init) ?? "--", unitKey: "unit_bpm")
                Text("summary_sent_hint").font(.caption2).foregroundStyle(.secondary)
                Button("action_done", action: onDone)
            }
        }
    }
}

private let sampleWorkout = WatchWorkout(
    id: "preview", activityType: "running", startTime: 0, endTime: 1_800_000, distanceMeters: 5230,
    elevationGainMeters: 64, averageHeartRate: 148, maxHeartRate: 172, trackpoints: [])

#Preview("Summary 41mm") {
    WorkoutSummaryView(workout: sampleWorkout, onDone: {}).frame(width: 176, height: 215)
}

#Preview("Summary 49mm dark") {
    WorkoutSummaryView(workout: sampleWorkout, onDone: {})
        .frame(width: 205, height: 251)
        .preferredColorScheme(.dark)
}
