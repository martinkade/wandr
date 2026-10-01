import SwiftUI
import shared

struct LiveGpsTrackingView: View {
    @StateObject private var locationTracker = LocationTrackingManager()
    @State private var isTracking: Bool = false
    @State private var isPaused: Bool = false
    @State private var distanceMeters: Double = 0.0
    @State private var durationSeconds: Double = 0.0
    @State private var trackpoints: [GpsTrackpoint] = []

    var onStopAndSave: (Double, Double, [GpsTrackpoint]) -> Void = { _, _, _ in }

    var body: some View {
        VStack(spacing: 20) {
            Text(LocalizedStringKey("live_gps_tracking_title"))
                .font(.title2)
                .bold()

            TrackMapView(trackpoints: trackpoints)

            HStack {
                VStack {
                    Text("DIST (KM)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text(String(format: "%.2f", distanceMeters / 1000.0))
                        .font(.system(size: 36, weight: .bold))
                        .foregroundColor(.blue)
                }
                .frame(maxWidth: .infinity)

                VStack {
                    Text("TIME (MIN)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    let mins = Int(durationSeconds / 60.0)
                    let secs = Int(durationSeconds.truncatingRemainder(dividingBy: 60.0))
                    Text(String(format: "%02d:%02d", mins, secs))
                        .font(.system(size: 36, weight: .bold))
                        .foregroundColor(.purple)
                }
                .frame(maxWidth: .infinity)
            }
            .padding()
            .background(.ultraThinMaterial)
            .cornerRadius(16)

            Spacer()

            if !isTracking {
                Button(action: {
                    isTracking = true
                    locationTracker.startTracking()
                }) {
                    Text(LocalizedStringKey("start_tracking_button"))
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 54)
                        .background(Color.blue)
                        .cornerRadius(14)
                }
            } else {
                HStack(spacing: 12) {
                    Button(action: {
                        isPaused.toggle()
                    }) {
                        Text(isPaused ? LocalizedStringKey("resume_button") : LocalizedStringKey("pause_button"))
                            .font(.headline)
                            .foregroundColor(.primary)
                            .frame(maxWidth: .infinity)
                            .frame(height: 54)
                            .background(.ultraThinMaterial)
                            .cornerRadius(14)
                    }

                    Button(action: {
                        isTracking = false
                        locationTracker.stopTracking()
                        onStopAndSave(distanceMeters, durationSeconds, trackpoints)
                    }) {
                        Text(LocalizedStringKey("stop_save_button"))
                            .font(.headline)
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 54)
                            .background(Color.red)
                            .cornerRadius(14)
                    }
                }
            }
        }
        .padding(16)
        .onReceive(locationTracker.$currentTrackpoint) { tp in
            guard let tp = tp, isTracking, !isPaused else { return }
            trackpoints.append(tp)
        }
    }
}

#Preview("Light Mode") {
    LiveGpsTrackingView()
        .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    LiveGpsTrackingView()
        .preferredColorScheme(.dark)
}
