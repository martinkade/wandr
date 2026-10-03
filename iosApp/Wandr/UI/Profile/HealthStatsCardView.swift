import SwiftUI

/// Today's steps and floors climbed from Apple Health. Only shown when the connection is enabled.
struct HealthStatsCardView: View {
    let summary: DailyHealthSummary

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(LocalizedStringKey("health_stats_title"))
                .font(.headline)
            stat(summary.steps.formatted(), "health_stats_steps")
            stat(summary.floors.formatted(), "health_stats_floors")
            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .padding(16)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16))
        .accessibilityElement(children: .combine)
    }

    private func stat(_ value: String, _ label: LocalizedStringKey) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(value).font(.title.weight(.semibold)).foregroundStyle(Color.wandrAccentText)
            Text(label).font(.caption).foregroundStyle(.secondary)
        }
    }
}

#Preview("Light Mode") {
    HealthStatsCardView(summary: DailyHealthSummary(steps: 8432, floors: 12))
        .frame(width: 300, height: 240).padding().preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    HealthStatsCardView(summary: DailyHealthSummary(steps: 8432, floors: 12))
        .frame(width: 300, height: 240).padding().preferredColorScheme(.dark)
}

#Preview("Large text") {
    HealthStatsCardView(summary: DailyHealthSummary(steps: 8432, floors: 12))
        .frame(width: 300, height: 240).padding().dynamicTypeSize(.accessibility2)
}
