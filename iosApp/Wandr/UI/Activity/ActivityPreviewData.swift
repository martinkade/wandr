import Foundation
@preconcurrency import shared

// Plain sample values for the previews (no network, no Koin).

extension Activity {
    static var sample: Activity { sample() }

    static func sample(
        id: String = "a1", userId: String = "u1", title: String = "Weekend Trail Walk", description: String? = "Along the river, then up to the castle.",
        type: String = "hiking", polyline: String? = "_p~iF~ps|U_ulLnnqC_mqNvxq`@", showMap: Bool = true, isManualEntry: Bool = false
    ) -> Activity {
        Activity(
            id: id, userId: userId, teamId: "t1", title: title, description: description,
            activityType: type, distanceMeters: 9_160, durationSeconds: 3_067, elevationGainMeters: 185,
            fitFilePath: nil, startTime: 1_768_435_200_000, endTime: 1_768_438_267_000,
            isManualEntry: isManualEntry, createdAt: 0, updatedAt: 0,
            showMap: showMap, polyline: polyline
        )
    }
}

extension ActivityForm {
    static func sample(editing: Bool = false, measured: Bool = false) -> ActivityForm {
        ActivityForm(
            activityId: editing ? "a1" : nil, title: editing ? "Evening run" : "", description: "",
            activityType: editing ? "running" : "hiking", distanceKm: 10, durationMinutes: 50, elevationMeters: 85,
            startTime: 1_768_435_200_000, isMeasured: measured, showMap: true, hasRoute: measured
        )
    }
}
