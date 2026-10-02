import Foundation

/// A workout recorded on the watch and sent to the phone as JSON.
/// Wire format identical to `shared/.../domain/watch/WatchWorkout.kt` (snake_case keys, epoch milliseconds).
struct WatchWorkout: Codable, Equatable, Sendable {
    static let currentVersion = 1

    var id: String
    /// `hiking`, `running` or `cycling`.
    var activityType: String
    var startTime: Int64
    var endTime: Int64
    var distanceMeters: Double
    var elevationGainMeters: Double
    var averageHeartRate: Int?
    var maxHeartRate: Int?
    var trackpoints: [WatchTrackpoint]
    var version: Int = WatchWorkout.currentVersion

    enum CodingKeys: String, CodingKey {
        case id
        case activityType = "activity_type"
        case startTime = "start_time"
        case endTime = "end_time"
        case distanceMeters = "distance_meters"
        case elevationGainMeters = "elevation_gain_meters"
        case averageHeartRate = "average_heart_rate"
        case maxHeartRate = "max_heart_rate"
        case trackpoints
        case version
    }

    func jsonString() throws -> String {
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.sortedKeys]
        let data = try encoder.encode(self)
        return String(decoding: data, as: UTF8.self)
    }

    static func decode(_ text: String) -> WatchWorkout? {
        try? JSONDecoder().decode(WatchWorkout.self, from: Data(text.utf8))
    }
}

struct WatchTrackpoint: Codable, Equatable, Sendable {
    var latitude: Double
    var longitude: Double
    var altitudeMeters: Double
    /// Epoch milliseconds.
    var timestamp: Int64
    var speedMetersPerSecond: Double

    enum CodingKeys: String, CodingKey {
        case latitude = "lat"
        case longitude = "lon"
        case altitudeMeters = "alt"
        case timestamp = "t"
        case speedMetersPerSecond = "speed"
    }
}
