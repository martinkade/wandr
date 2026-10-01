import SwiftUI
import MapKit
import shared

struct TrackMapView: View {
    let trackpoints: [GpsTrackpoint]

    var body: some View {
        Map {
            if trackpoints.count >= 2 {
                MapPolyline(coordinates: trackpoints.map { CLLocationCoordinate2D(latitude: $0.latitude, longitude: $0.longitude) })
                    .stroke(.blue, lineWidth: 4)
            }
        }
        .frame(height: 200)
        .cornerRadius(12)
    }
}

#Preview("Light Mode") {
    TrackMapView(trackpoints: [
        GpsTrackpoint(latitude: 47.3769, longitude: 8.5417, altitudeMeters: 400.0, timestamp: 0, speedMetersPerSecond: 0.0),
        GpsTrackpoint(latitude: 47.3779, longitude: 8.5437, altitudeMeters: 410.0, timestamp: 0, speedMetersPerSecond: 0.0)
    ])
    .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    TrackMapView(trackpoints: [
        GpsTrackpoint(latitude: 47.3769, longitude: 8.5417, altitudeMeters: 400.0, timestamp: 0, speedMetersPerSecond: 0.0),
        GpsTrackpoint(latitude: 47.3779, longitude: 8.5437, altitudeMeters: 410.0, timestamp: 0, speedMetersPerSecond: 0.0)
    ])
    .preferredColorScheme(.dark)
}
