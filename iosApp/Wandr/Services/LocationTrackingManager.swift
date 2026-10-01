import Foundation
import CoreLocation
import Combine
import shared

final class LocationTrackingManager: NSObject, ObservableObject, CLLocationManagerDelegate {
    @Published var currentTrackpoint: GpsTrackpoint?
    @Published var isTracking: Bool = false
    
    private let locationManager = CLLocationManager()
    
    override init() {
        super.init()
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.allowsBackgroundLocationUpdates = true
        locationManager.pausesLocationUpdatesAutomatically = false
    }
    
    func startTracking() {
        locationManager.requestAlwaysAuthorization()
        locationManager.startUpdatingLocation()
        isTracking = true
    }
    
    func stopTracking() {
        locationManager.stopUpdatingLocation()
        isTracking = false
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
