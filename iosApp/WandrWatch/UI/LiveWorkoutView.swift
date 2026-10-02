import SwiftUI

/// Live metrics with pause / resume / stop.
struct LiveWorkoutView: View {
    var kind: WorkoutKind
    var metrics: WorkoutMetrics
    var isPaused: Bool
    var elapsed: (Date) -> TimeInterval
    var onPause: () -> Void
    var onResume: () -> Void
    var onStop: () -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 6) {
                TimelineView(.periodic(from: .now, by: 1)) { context in
                    Text(MetricFormatter.duration(elapsed(context.date)))
                        .font(.title2.weight(.semibold))
                        .monospacedDigit()
                        .foregroundStyle(isPaused ? Color.secondary : Color.accentColor)
                }
                MetricRow(titleKey: "metric_distance", value: MetricFormatter.kilometers(metrics.distanceMeters), unitKey: "unit_km")
                MetricRow(titleKey: "metric_elevation", value: MetricFormatter.meters(metrics.elevationGainMeters), unitKey: "unit_m")
                MetricRow(titleKey: "metric_heart_rate", value: metrics.currentHeartRate.map(String.init) ?? "--", unitKey: "unit_bpm")
                HStack {
                    if isPaused {
                        Button(action: onResume) { Image(systemName: "play.fill") }
                            .accessibilityLabel("action_resume")
                    } else {
                        Button(action: onPause) { Image(systemName: "pause.fill") }
                            .accessibilityLabel("action_pause")
                    }
                    Button(role: .destructive, action: onStop) { Image(systemName: "stop.fill") }
                        .accessibilityLabel("action_stop")
                }
            }
        }
        .navigationTitle(LocalizedStringKey(kind.titleKey))
    }
}

private func sampleMetrics() -> WorkoutMetrics {
    var m = WorkoutMetrics()
    m.addHeartRate(132)
    m.add(LocationSample(latitude: 48.0, longitude: 11.0, altitude: 500, horizontalAccuracy: 5, verticalAccuracy: 5, speed: 1.4, timestamp: .now))
    m.add(LocationSample(latitude: 48.01, longitude: 11.0, altitude: 520, horizontalAccuracy: 5, verticalAccuracy: 5, speed: 1.4, timestamp: .now))
    return m
}

#Preview("Live 41mm") {
    LiveWorkoutView(kind: .hiking, metrics: sampleMetrics(), isPaused: false, elapsed: { _ in 754 },
                    onPause: {}, onResume: {}, onStop: {})
        .frame(width: 176, height: 215)
}

#Preview("Live paused 49mm dark") {
    LiveWorkoutView(kind: .cycling, metrics: sampleMetrics(), isPaused: true, elapsed: { _ in 3725 },
                    onPause: {}, onResume: {}, onStop: {})
        .frame(width: 205, height: 251)
        .preferredColorScheme(.dark)
}
