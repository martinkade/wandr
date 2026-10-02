import Foundation
import XCTest
@testable import WandrWatch

final class WatchWorkoutTests: XCTestCase {
    private func sample() -> WatchWorkout {
        WatchWorkout(id: "abc", activityType: "hiking", startTime: 1000, endTime: 5000, distanceMeters: 1234.5,
                     elevationGainMeters: 40, averageHeartRate: 120, maxHeartRate: 150,
                     trackpoints: [WatchTrackpoint(latitude: 48, longitude: 11, altitudeMeters: 500, timestamp: 2000, speedMetersPerSecond: 1.5)])
    }

    func testJsonKeysMatchWireFormat() throws {
        let object = try XCTUnwrap(JSONSerialization.jsonObject(with: Data(sample().jsonString().utf8)) as? [String: Any])
        XCTAssertEqual(Set(object.keys), ["id", "activity_type", "start_time", "end_time", "distance_meters",
                                           "elevation_gain_meters", "average_heart_rate", "max_heart_rate", "trackpoints", "version"])
        XCTAssertEqual(object["version"] as? Int, 1)
        let point = try XCTUnwrap((object["trackpoints"] as? [[String: Any]])?.first)
        XCTAssertEqual(Set(point.keys), ["lat", "lon", "alt", "t", "speed"])
    }

    func testRoundTrip() throws {
        let w = sample()
        XCTAssertEqual(WatchWorkout.decode(try w.jsonString()), w)
    }

    func testMetricsAccumulate() {
        var m = WorkoutMetrics()
        let t = Date(timeIntervalSince1970: 100)
        m.add(LocationSample(latitude: 48.0, longitude: 11, altitude: 500, horizontalAccuracy: 5, verticalAccuracy: 5, speed: 1, timestamp: t))
        m.add(LocationSample(latitude: 48.001, longitude: 11, altitude: 510, horizontalAccuracy: 5, verticalAccuracy: 5, speed: 1, timestamp: t))
        m.add(LocationSample(latitude: 48.002, longitude: 11, altitude: 500, horizontalAccuracy: 500, verticalAccuracy: 5, speed: 1, timestamp: t))
        XCTAssertEqual(m.distanceMeters, 111.2, accuracy: 1)
        XCTAssertEqual(m.elevationGainMeters, 10, accuracy: 0.001)
        XCTAssertEqual(m.trackpoints.count, 2)
        m.addHeartRate(100); m.addHeartRate(140)
        XCTAssertEqual(m.averageHeartRate, 120)
        XCTAssertEqual(m.maxHeartRate, 140)
    }

    func testOutboxAddRemove() {
        let url = FileManager.default.temporaryDirectory.appendingPathComponent("outbox-\(UUID().uuidString).json")
        let outbox = WorkoutOutbox(fileURL: url)
        outbox.add(id: "1", json: "{}")
        XCTAssertEqual(outbox.pending(), ["1": "{}"])
        outbox.remove(id: "1")
        XCTAssertTrue(outbox.pending().isEmpty)
    }
}
