import SwiftUI
@preconcurrency import shared

/// Distance, time, pace (speed on a bike) and elevation gain; a 2-column grid when one row does not fit (large text).
struct ActivityStatsView: View {
    let activityType: String
    let distanceMeters: Double
    let durationSeconds: Double
    let elevationGainMeters: Double

    private struct Stat: Identifiable {
        let id: String
        let label: LocalizedStringKey
        let value: String
    }

    private var stats: [Stat] {
        let isCycling = activityType == "cycling"
        var list = [
            Stat(id: "distance", label: "activity_stat_distance", value: ActivityFormat.distanceKm(distanceMeters)),
            Stat(id: "time", label: "activity_stat_time", value: ActivityFormat.duration(durationSeconds))
        ]
        let pace = isCycling
            ? ActivityFormat.speed(distanceMeters: distanceMeters, durationSeconds: durationSeconds)
            : ActivityFormat.pace(distanceMeters: distanceMeters, durationSeconds: durationSeconds)
        if let pace {
            list.append(Stat(id: "pace", label: isCycling ? "activity_stat_speed" : "activity_stat_pace", value: pace))
        }
        if elevationGainMeters > 0 {
            list.append(Stat(id: "elevation", label: "activity_stat_elevation", value: ActivityFormat.elevation(elevationGainMeters)))
        }
        return list
    }

    var body: some View {
        ViewThatFits(in: .horizontal) {
            HStack(alignment: .top, spacing: 16) {
                ForEach(stats) { cell($0).fixedSize() }
                Spacer(minLength: 0)
            }
            Grid(alignment: .leading, horizontalSpacing: 16, verticalSpacing: 12) {
                ForEach(Array(stride(from: 0, to: stats.count, by: 2)), id: \.self) { index in
                    GridRow {
                        cell(stats[index])
                        if index + 1 < stats.count { cell(stats[index + 1]) }
                    }
                }
            }
        }
    }

    private func cell(_ stat: Stat) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(stat.label)
                .font(.caption)
                .foregroundStyle(.secondary)
            Text(stat.value)
                .font(.title3.weight(.semibold))
                .fontDesign(.rounded)
                .monospacedDigit()
        }
        .accessibilityElement(children: .combine)
    }
}

#Preview("Light Mode") {
    VStack(alignment: .leading, spacing: 24) {
        ActivityStatsView(activityType: "running", distanceMeters: 9_160, durationSeconds: 3_067, elevationGainMeters: 85)
        ActivityStatsView(activityType: "cycling", distanceMeters: 42_300, durationSeconds: 5_400, elevationGainMeters: 0)
    }
    .padding().preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    ActivityStatsView(activityType: "hiking", distanceMeters: 12_400, durationSeconds: 14_000, elevationGainMeters: 820)
        .padding().preferredColorScheme(.dark)
}

#Preview("Large Text") {
    ActivityStatsView(activityType: "running", distanceMeters: 9_160, durationSeconds: 3_067, elevationGainMeters: 85)
        .padding().environment(\.dynamicTypeSize, .accessibility3)
}
