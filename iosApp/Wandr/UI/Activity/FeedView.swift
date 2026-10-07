import SwiftUI
import shared

struct FeedView: View {
    let activities: [Activity]
    var onSelectActivity: (Activity) -> Void = { _ in }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            if activities.isEmpty {
                Text("No activities recorded yet.")
                    .font(.body)
                    .foregroundColor(.secondary)
                    .padding(.top, 20)
            } else {
                ScrollView {
                    LazyVStack(spacing: 12) {
                        ForEach(activities, id: \.id) { activity in
                            ActivityCardView(
                                title: activity.title,
                                description: activity.description_,
                                distanceMeters: activity.distanceMeters,
                                durationSeconds: activity.durationSeconds,
                                elevationGainMeters: activity.elevationGainMeters,
                                isManualEntry: activity.isManualEntry,
                                route: activity.route,
                                onClick: { onSelectActivity(activity) }
                            )
                        }
                    }
                }
            }
        }
        .padding(16)
    }
}

#Preview("Light Mode") {
    FeedView(activities: [
        Activity(
            id: "a1",
            userId: "u1",
            teamId: "t1",
            title: "Weekend Trail Walk",
            description: nil,
            activityType: "hiking",
            distanceMeters: 5400.0,
            durationSeconds: 3600.0,
            elevationGainMeters: 150.0,
            fitFilePath: nil,
            startTime: 0,
            endTime: 0,
            isManualEntry: true,
            createdAt: 0,
            updatedAt: 0,
            showMap: true,
            polyline: nil
        )
    ])
    .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    FeedView(activities: [
        Activity(
            id: "a1",
            userId: "u1",
            teamId: "t1",
            title: "Weekend Trail Walk",
            description: nil,
            activityType: "hiking",
            distanceMeters: 5400.0,
            durationSeconds: 3600.0,
            elevationGainMeters: 150.0,
            fitFilePath: nil,
            startTime: 0,
            endTime: 0,
            isManualEntry: true,
            createdAt: 0,
            updatedAt: 0,
            showMap: true,
            polyline: nil
        )
    ])
    .preferredColorScheme(.dark)
}
