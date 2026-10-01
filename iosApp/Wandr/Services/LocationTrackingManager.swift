import Foundation
import CoreLocation
import Combine
import shared

final class LocationTrackingManager: NSObject, ObservableObject, CLLocationManagerDelegate {
    @Published var currentTrackpoint: GpsTrackpoint?
    @Published var isTracking: Bool = false
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
    
    /// Call only after the location disclosure was confirmed (or access is already granted).
    func startTracking() {
        // "While Using" is enough for background recording with the `location` background mode.
        locationManager.requestWhenInUseAuthorization()
        locationManager.startUpdatingLocation()
        isTracking = true
    }
    
    func stopTracking() {
        locationManager.stopUpdatingLocation()
        isTracking = false
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
        currentTrackpoint = trackpoint
    }
}
