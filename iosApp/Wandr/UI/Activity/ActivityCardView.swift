import SwiftUI

struct ActivityCardView: View {
    let title: String
    let description: String?
    let distanceMeters: Double
    let durationSeconds: Double
    let elevationGainMeters: Double
    let isManualEntry: Bool
    var onClick: () -> Void = {}

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(title)
                    .font(.headline)
                    .foregroundColor(.primary)
                Spacer()
                Text(isManualEntry ? LocalizedStringKey("manual_entry") : LocalizedStringKey("gps_tracked"))
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundColor(.blue)
            }

            if let desc = description {
                Text(desc)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }

            HStack {
                let km = distanceMeters / 1000.0
                let mins = Int(durationSeconds / 60.0)

                Label(String(format: "%.2f km", km), systemImage: "ruler")
                    .font(.subheadline)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Label("\(mins) min", systemImage: "clock")
                    .font(.subheadline)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Label("\(Int(elevationGainMeters)) m", systemImage: "mountain.2")
                    .font(.subheadline)
            }
        }
        .padding(16)
        .background(.ultraThinMaterial)
        .cornerRadius(16)
        .onTapGesture {
            onClick()
        }
    }
}

#Preview("Light Mode") {
    ActivityCardView(
        title: "Morning Trail Hike",
        description: "Scenic hike up the mountain trail.",
        distanceMeters: 8500.0,
        durationSeconds: 5400.0,
        elevationGainMeters: 340.0,
        isManualEntry: false
    )
    .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    ActivityCardView(
        title: "Morning Trail Hike",
        description: "Scenic hike up the mountain trail.",
        distanceMeters: 8500.0,
        durationSeconds: 5400.0,
        elevationGainMeters: 340.0,
        isManualEntry: false
    )
    .preferredColorScheme(.dark)
}
