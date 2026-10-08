import SwiftUI
@preconcurrency import shared

/// Records an activity with the GPS: the route on the map, time, distance and pace, pause / resume and finish (save or
/// discard). The recording itself lives in the shared recording view model and runs in the `RecordingCoordinator`, so this
/// screen can be closed while it runs and opened again. Before the start the GPS status and position are shown. While a
/// recording runs the Live Activity shows it on the lock screen and in the Dynamic Island.
struct LiveGpsTrackingView: View {
    let userId: String
    let teamId: String?
    var onClose: () -> Void = {}

    @ObservedObject private var locationTracker = LocationTrackingManager.shared
    @StateObject private var recording = RecordingObserver()
    @State private var activityType = "hiking"
    @State private var showLocationDisclosure = false
    @State private var showLocationDenied = false
    @State private var showFinishDialog = false
    private let coordinator = RecordingCoordinator.shared

    private var state: ActivityState { recording.state }

    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                // Drag and pinch; it follows the position until the user moves it (then a button brings it back).
                InteractiveMapView(route: state.isTracking ? state.liveTrackpoints : [], position: mapPosition, height: 280)

                gpsStatusRow

                if !state.isTracking {
                    Picker(LocalizedStringKey("recording_type_label"), selection: $activityType) {
                        Text(LocalizedStringKey("activity_type_hiking")).tag("hiking")
                        Text(LocalizedStringKey("activity_type_running")).tag("running")
                        Text(LocalizedStringKey("activity_type_cycling")).tag("cycling")
                    }
                    .pickerStyle(.segmented)
                } else {
                    figures
                }

                if let error = state.error {
                    Text(error.userMessage).font(.footnote).foregroundColor(.red)
                }

                Spacer()
                controls
            }
            .padding(16)
            .navigationTitle(LocalizedStringKey("live_gps_tracking_title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                // A running recording goes on in the background; the feed's record button opens it again.
                ToolbarItem(placement: .topBarLeading) {
                    Button { leave() } label: { Image(systemName: "chevron.down") }
                        .accessibilityLabel(Text(LocalizedStringKey("recording_close")))
                }
            }
        }
        .permissionDisclosure(
            isPresented: $showLocationDisclosure,
            title: "permission_location_title",
            message: "permission_location_message",
            onContinue: { locationTracker.requestAuthorization() }
        )
        .permissionDeniedAlert(isPresented: $showLocationDenied, message: "permission_location_denied")
        .confirmationDialog(
            LocalizedStringKey("recording_finish_title"), isPresented: $showFinishDialog, titleVisibility: .visible
        ) {
            Button(LocalizedStringKey("save_activity_button")) { saveRecording() }
            Button(LocalizedStringKey("recording_discard_button"), role: .destructive) { discardRecording() }
            Button(LocalizedStringKey("recording_continue_button"), role: .cancel) { coordinator.resume() }
        } message: {
            Text(LocalizedStringKey("recording_finish_message"))
        }
        .sheet(isPresented: Binding(get: { state.conflict != nil }, set: { _ in })) {
            if let conflict = state.conflict {
                ActivityConflictView(conflict: conflict, isSaving: state.isSaving) { recording.send(ActivityIntentResolveConflict(resolution: $0)) }
            }
        }
        .task { recording.start() }
        // Before the start the GPS is watched for status and position; the permission is asked for when the screen opens.
        .onAppear { prepareGps() }
        .onChange(of: locationTracker.authorizationStatus) { _, _ in prepareGps() }
        // Saved (or merged / trimmed / discarded): the recording is over.
        .onChange(of: state.success) { _, success in
            guard let success, [.recorded, .discarded, .merged, .trimmed].contains(success) else { return }
            recording.send(ActivityIntentClearMessages.shared)
            coordinator.endSession()
            onClose()
        }
    }

    /// Where the user is: the latest point of the recording, before the start the current GPS position.
    private var mapPosition: GpsTrackpoint? {
        state.isTracking ? state.liveTrackpoints.last : locationTracker.currentTrackpoint
    }

    private var gpsStatusRow: some View {
        HStack(spacing: 8) {
            Image(systemName: "location.fill").font(.footnote)
            Text(LocalizedStringKey(gpsStatusKey)).font(.subheadline.weight(.semibold))
            Spacer()
        }
        .foregroundColor(state.gpsStatus == .good ? .wandrAccentText : .secondary)
    }

    private var gpsStatusKey: String {
        switch state.gpsStatus {
        case .searching: return "recording_gps_searching"
        case .weak: return "recording_gps_weak"
        case .good: return "recording_gps_good"
        default: return "recording_gps_searching"
        }
    }

    private var figures: some View {
        HStack {
            figure(String(format: "%.2f", state.liveDistanceMeters / 1000.0), "recording_distance_label")
            figure(clockText(state.liveDurationSeconds), "recording_time_label")
            figure(paceText(state.liveCurrentPaceSecondsPerKm?.doubleValue), "recording_pace_label")
        }
        .padding()
        .background(.ultraThinMaterial)
        .cornerRadius(16)
    }

    private func figure(_ value: String, _ label: String) -> some View {
        VStack {
            Text(value).font(.system(size: 28, weight: .bold)).foregroundColor(.wandrAccentText).monospacedDigit()
            Text(LocalizedStringKey(label)).font(.caption).foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
    }

    @ViewBuilder
    private var controls: some View {
        if !state.isTracking {
            Button(action: startTrackingIfPermitted) {
                Text(LocalizedStringKey("start_tracking_button"))
                    .font(.headline)
                    .foregroundColor(.wandrOnPrimary)
                    .frame(maxWidth: .infinity)
                    .frame(height: 54)
                    .background(Color.wandrPrimary)
                    .cornerRadius(14)
            }
        } else {
            HStack(spacing: 12) {
                Button {
                    if state.isPaused { coordinator.resume() } else { coordinator.pause() }
                } label: {
                    Text(state.isPaused ? LocalizedStringKey("resume_button") : LocalizedStringKey("pause_button"))
                        .font(.headline)
                        .foregroundColor(.primary)
                        .frame(maxWidth: .infinity)
                        .frame(height: 54)
                        .background(.ultraThinMaterial)
                        .cornerRadius(14)
                }
                Button {
                    // The clock stops while the user decides.
                    coordinator.pause()
                    showFinishDialog = true
                } label: {
                    Text(LocalizedStringKey("recording_finish_button"))
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
}

extension LiveGpsTrackingView {
    /// Disclosure first, then the system prompt; denied access leads to Settings.
    private func startTrackingIfPermitted() {
        switch locationTracker.authorizationStatus {
        case .notDetermined: showLocationDisclosure = true
        case .denied, .restricted: showLocationDenied = true
        default: beginTracking()
        }
    }

    /// Starts watching the GPS (status and position) when access is granted; asks for it first when it was never asked.
    private func prepareGps() {
        switch locationTracker.authorizationStatus {
        case .notDetermined: if !state.isTracking { showLocationDisclosure = true }
        case .authorizedWhenInUse, .authorizedAlways: locationTracker.startPreview()
        default: break
        }
    }

    private func beginTracking() {
        coordinator.start(activityType: activityType)
    }

    private func saveRecording() {
        let title = NSLocalizedString("activity_type_\(state.trackingActivityType)", comment: "")
        coordinator.save(userId: userId, teamId: teamId, title: title)
    }

    private func discardRecording() {
        coordinator.discard()
        onClose()
    }

    /// Leaves the screen; a running recording goes on, otherwise the GPS preview stops.
    private func leave() {
        if !state.isTracking { locationTracker.stopPreview() }
        onClose()
    }

    private func clockText(_ seconds: Double) -> String {
        let total = Int(seconds)
        let hours = total / 3600
        let minutes = (total % 3600) / 60
        let secs = total % 60
        return hours > 0 ? String(format: "%d:%02d:%02d", hours, minutes, secs) : String(format: "%02d:%02d", minutes, secs)
    }

    private func paceText(_ secondsPerKm: Double?) -> String {
        guard let secondsPerKm, secondsPerKm > 0, secondsPerKm < 3600 else { return "–" }
        return String(format: "%d:%02d", Int(secondsPerKm) / 60, Int(secondsPerKm) % 60)
    }
}

#Preview("Light Mode") {
    LiveGpsTrackingView(userId: "u1", teamId: nil)
        .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    LiveGpsTrackingView(userId: "u1", teamId: nil)
        .preferredColorScheme(.dark)
}
