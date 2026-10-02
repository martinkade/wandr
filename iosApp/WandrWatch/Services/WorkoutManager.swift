import CoreLocation
import Foundation
import HealthKit
import Observation

enum WorkoutPhase: Equatable {
    case idle
    case running
    case paused
    case summary(WatchWorkout)
}

/// Drives an `HKWorkoutSession` + `HKLiveWorkoutBuilder`, collects the GPS route and builds the [WatchWorkout].
@MainActor
@Observable
final class WorkoutManager: NSObject {
    private(set) var phase: WorkoutPhase = .idle
    private(set) var kind: WorkoutKind = .hiking
    private(set) var metrics = WorkoutMetrics()
    private(set) var permission: PermissionState
    private(set) var startDate: Date?
    private(set) var errorMessageKey: String?

    private let healthStore = HKHealthStore()
    private let locationManager = CLLocationManager()
    private let connectivity: WatchConnectivityService
    private var session: HKWorkoutSession?
    private var builder: HKLiveWorkoutBuilder?
    private var routeBuilder: HKWorkoutRouteBuilder?
    private var pendingKind: WorkoutKind?

    init(connectivity: WatchConnectivityService = .shared) {
        self.connectivity = connectivity
        permission = PermissionState.current(location: locationManager.authorizationStatus)
        super.init()
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.activityType = .fitness
    }

    /// Seconds of active time as shown on screen, derived from the builder.
    func elapsed(at date: Date) -> TimeInterval {
        builder?.elapsedTime(at: date) ?? 0
    }

    // MARK: Authorization

    func refreshPermission() {
        permission = PermissionState.current(location: locationManager.authorizationStatus)
    }

    /// Called from the picker. Starts directly if permitted, otherwise the UI shows the disclosure first.
    func requestStart(_ kind: WorkoutKind) {
        pendingKind = kind
        refreshPermission()
        if permission == .granted { start(kind) }
    }

    func cancelPending() { pendingKind = nil }

    var hasPendingStart: Bool { pendingKind != nil }

    /// Only call after the disclosure was confirmed by the user.
    func requestAuthorizationAfterDisclosure() async {
        let share: Set<HKSampleType> = [HKObjectType.workoutType(), HKSeriesType.workoutRoute()]
        let read: Set<HKObjectType> = [
            HKQuantityType(.heartRate), HKQuantityType(.distanceWalkingRunning), HKQuantityType(.distanceCycling)
        ]
        do {
            try await healthStore.requestAuthorization(toShare: share, read: read)
        } catch {
            errorMessageKey = "error_health_authorization"
        }
        locationManager.requestWhenInUseAuthorization()
        // Location answers asynchronously via the delegate; continue there.
        if locationManager.authorizationStatus != .notDetermined { finishAuthorization() }
    }

    private func finishAuthorization() {
        refreshPermission()
        if permission == .granted, let kind = pendingKind { start(kind) }
        else if permission == .denied { pendingKind = nil }
    }

    // MARK: Workout lifecycle

    func start(_ kind: WorkoutKind) {
        pendingKind = nil
        self.kind = kind
        metrics = WorkoutMetrics()
        errorMessageKey = nil

        let configuration = HKWorkoutConfiguration()
        configuration.activityType = kind.healthKitType
        configuration.locationType = .outdoor

        do {
            let session = try HKWorkoutSession(healthStore: healthStore, configuration: configuration)
            let builder = session.associatedWorkoutBuilder()
            builder.dataSource = HKLiveWorkoutDataSource(healthStore: healthStore, workoutConfiguration: configuration)
            session.delegate = self
            builder.delegate = self
            self.session = session
            self.builder = builder
            routeBuilder = HKWorkoutRouteBuilder(healthStore: healthStore, device: nil)

            let date = Date()
            startDate = date
            session.startActivity(with: date)
            Task { try? await builder.beginCollection(at: date) }
            locationManager.allowsBackgroundLocationUpdates = true
            locationManager.startUpdatingLocation()
            phase = .running
        } catch {
            errorMessageKey = "error_start_failed"
        }
    }

    func pause() { session?.pause() }
    func resume() { session?.resume() }

    func stop() {
        session?.end()
    }

    func dismissSummary() {
        phase = .idle
        startDate = nil
        metrics = WorkoutMetrics()
    }

    private func finish() async {
        locationManager.stopUpdatingLocation()
        locationManager.allowsBackgroundLocationUpdates = false
        let end = Date()
        guard let builder, let start = startDate else { return }
        do {
            try await builder.endCollection(at: end)
            let hkWorkout = try await builder.finishWorkout()
            if let hkWorkout { try? await routeBuilder?.finishRoute(with: hkWorkout, metadata: nil) }
        } catch {
            errorMessageKey = "error_save_failed"
        }
        let workout = metrics.workout(kind: kind, start: start, end: end)
        connectivity.send(workout)
        session = nil
        self.builder = nil
        routeBuilder = nil
        phase = .summary(workout)
    }
}

extension WorkoutManager: HKWorkoutSessionDelegate {
    nonisolated func workoutSession(_ workoutSession: HKWorkoutSession, didChangeTo toState: HKWorkoutSessionState,
                                    from fromState: HKWorkoutSessionState, date: Date) {
        Task { @MainActor in
            switch toState {
            case .running: phase = .running
            case .paused: phase = .paused
            case .ended: await finish()
            default: break
            }
        }
    }

    nonisolated func workoutSession(_ workoutSession: HKWorkoutSession, didFailWithError error: Error) {
        Task { @MainActor in errorMessageKey = "error_start_failed" }
    }
}

extension WorkoutManager: HKLiveWorkoutBuilderDelegate {
    nonisolated func workoutBuilderDidCollectEvent(_ workoutBuilder: HKLiveWorkoutBuilder) {}

    nonisolated func workoutBuilder(_ workoutBuilder: HKLiveWorkoutBuilder, didCollectDataOf collectedTypes: Set<HKSampleType>) {
        guard collectedTypes.contains(HKQuantityType(.heartRate)),
              let stats = workoutBuilder.statistics(for: HKQuantityType(.heartRate)),
              let bpm = stats.mostRecentQuantity()?.doubleValue(for: HKUnit.count().unitDivided(by: .minute())) else { return }
        let value = Int(bpm.rounded())
        Task { @MainActor in metrics.addHeartRate(value) }
    }
}

extension WorkoutManager: CLLocationManagerDelegate {
    nonisolated func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        Task { @MainActor in
            guard phase == .running else { return }
            for location in locations {
                metrics.add(LocationSample(
                    latitude: location.coordinate.latitude, longitude: location.coordinate.longitude,
                    altitude: location.altitude, horizontalAccuracy: location.horizontalAccuracy,
                    verticalAccuracy: location.verticalAccuracy, speed: location.speed, timestamp: location.timestamp
                ))
            }
            try? await routeBuilder?.insertRouteData(locations)
        }
    }

    nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        Task { @MainActor in
            if pendingKind != nil { finishAuthorization() } else { refreshPermission() }
        }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {}
}
