import Foundation

/// A single location sample, independent of CoreLocation so it can be unit tested.
struct LocationSample: Equatable, Sendable {
    var latitude: Double
    var longitude: Double
    var altitude: Double
    var horizontalAccuracy: Double
    var verticalAccuracy: Double
    var speed: Double
    var timestamp: Date
}

/// Accumulates distance, elevation gain, heart rate and trackpoints of a running workout.
struct WorkoutMetrics: Equatable, Sendable {
    /// Samples less accurate than this (meters) are ignored.
    static let maxHorizontalAccuracy = 50.0
    /// Altitude changes below this threshold (meters) are treated as noise.
    static let elevationThreshold = 2.0

    private(set) var distanceMeters = 0.0
    private(set) var elevationGainMeters = 0.0
    private(set) var trackpoints: [WatchTrackpoint] = []
    private(set) var currentHeartRate: Int?
    private(set) var maxHeartRate: Int?
    private var heartRateSum = 0
    private var heartRateCount = 0
    private var last: LocationSample?
    private var referenceAltitude: Double?

    var averageHeartRate: Int? {
        heartRateCount == 0 ? nil : Int((Double(heartRateSum) / Double(heartRateCount)).rounded())
    }

    mutating func add(_ sample: LocationSample) {
        guard sample.horizontalAccuracy >= 0, sample.horizontalAccuracy <= Self.maxHorizontalAccuracy else { return }
        if let last {
            distanceMeters += Self.haversine(last.latitude, last.longitude, sample.latitude, sample.longitude)
        }
        if sample.verticalAccuracy >= 0 {
            if let ref = referenceAltitude {
                let delta = sample.altitude - ref
                if delta >= Self.elevationThreshold {
                    elevationGainMeters += delta
                    referenceAltitude = sample.altitude
                } else if delta <= -Self.elevationThreshold {
                    referenceAltitude = sample.altitude
                }
            } else {
                referenceAltitude = sample.altitude
            }
        }
        last = sample
        trackpoints.append(WatchTrackpoint(
            latitude: sample.latitude,
            longitude: sample.longitude,
            altitudeMeters: sample.altitude,
            timestamp: Int64((sample.timestamp.timeIntervalSince1970 * 1000).rounded()),
            speedMetersPerSecond: max(0, sample.speed)
        ))
    }

    mutating func addHeartRate(_ bpm: Int) {
        guard bpm > 0 else { return }
        currentHeartRate = bpm
        maxHeartRate = max(maxHeartRate ?? bpm, bpm)
        heartRateSum += bpm
        heartRateCount += 1
    }

    func workout(id: String = UUID().uuidString, kind: WorkoutKind, start: Date, end: Date) -> WatchWorkout {
        WatchWorkout(
            id: id,
            activityType: kind.wireValue,
            startTime: Int64((start.timeIntervalSince1970 * 1000).rounded()),
            endTime: Int64((end.timeIntervalSince1970 * 1000).rounded()),
            distanceMeters: distanceMeters,
            elevationGainMeters: elevationGainMeters,
            averageHeartRate: averageHeartRate,
            maxHeartRate: maxHeartRate,
            trackpoints: trackpoints
        )
    }

    static func haversine(_ lat1: Double, _ lon1: Double, _ lat2: Double, _ lon2: Double) -> Double {
        let r = 6_371_000.0
        let dLat = (lat2 - lat1) * .pi / 180
        let dLon = (lon2 - lon1) * .pi / 180
        let a = sin(dLat / 2) * sin(dLat / 2)
            + cos(lat1 * .pi / 180) * cos(lat2 * .pi / 180) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * r * asin(min(1, sqrt(a)))
    }
}
