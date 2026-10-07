import Foundation
import CoreLocation
import Combine
import shared

/// The device's GPS. One instance for the whole app: the recording keeps running when its screen is closed.
final class LocationTrackingManager: NSObject, ObservableObject, CLLocationManagerDelegate {
    nonisolated(unsafe) static let shared = LocationTrackingManager()

    @Published var currentTrackpoint: GpsTrackpoint?
    /// Accuracy (meters) of the latest fix; nil until the first one.
    @Published var horizontalAccuracy: Float?
    @Published var isTracking: Bool = false
    /// Fixes are delivered although nothing is recorded (GPS status and position before the start).
    @Published private(set) var isPreviewing: Bool = false
    @Published var authorizationStatus: CLAuthorizationStatus
    
    private let locationManager = CLLocationManager()
    
    override init() {
        authorizationStatus = CLLocationManager().authorizationStatus
        super.init()
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.allowsBackgroundLocationUpdates = true
        locationManager.pausesLocationUpdatesAutomatically = false
        locationManager.showsBackgroundLocationIndicator = true
    }
    
    /// Shows the system prompt; call only after the location disclosure was confirmed.
    func requestAuthorization() {
        locationManager.requestWhenInUseAuthorization()
    }

    /// Delivers fixes without recording, once the access is granted. Does nothing while a recording runs.
    func startPreview() {
        guard !isTracking, authorizationStatus == .authorizedWhenInUse || authorizationStatus == .authorizedAlways else { return }
        locationManager.startUpdatingLocation()
        isPreviewing = true
    }

    func stopPreview() {
        guard !isTracking else { return }
        locationManager.stopUpdatingLocation()
        isPreviewing = false
        horizontalAccuracy = nil
    }

    /// Call only after the location disclosure was confirmed (or access is already granted).
    func startTracking() {
        // "While Using" is enough for background recording with the `location` background mode.
        locationManager.requestWhenInUseAuthorization()
        locationManager.startUpdatingLocation()
        isTracking = true
        isPreviewing = false
    }
    
    func stopTracking() {
        locationManager.stopUpdatingLocation()
        isTracking = false
        isPreviewing = false
        horizontalAccuracy = nil
    }
    
    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        authorizationStatus = manager.authorizationStatus
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }
        let trackpoint = GpsTrackpoint(
            latitude: location.coordinate.latitude,
            longitude: location.coordinate.longitude,
            altitudeMeters: location.altitude,
            timestamp: Int64(location.timestamp.timeIntervalSince1970 * 1000),
            speedMetersPerSecond: Float(max(0, location.speed))
        )
        horizontalAccuracy = location.horizontalAccuracy >= 0 ? Float(location.horizontalAccuracy) : nil
        currentTrackpoint = trackpoint
    }
}
