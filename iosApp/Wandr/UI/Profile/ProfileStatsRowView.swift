import SwiftUI

/// "Active since" and the number of activities next to the Edit button.
struct ProfileStatsRowView: View {
    var memberSince: Date? = nil
    var activityCount: Int = 0
    var onEdit: () -> Void = {}

    var body: some View {
        ViewThatFits(in: .horizontal) {
            HStack(spacing: 24) {
                stats
                Spacer(minLength: 0)
                editButton
            }
            // Large text: the button goes below the stats.
            VStack(alignment: .leading, spacing: 16) {
                stats
                editButton
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .wandrCard()
    }

    private var stats: some View {
        HStack(alignment: .top, spacing: 24) {
            stat(
                label: "profile_stats_member_since",
                value: memberSince.map { $0.formatted(date: .abbreviated, time: .omitted) }
                    ?? String(localized: "empty_value_placeholder")
            )
            stat(label: "profile_stats_activities", value: activityCount.formatted())
        }
    }

    private var editButton: some View {
        Button(action: onEdit) {
            Label(LocalizedStringKey("profile_edit_button"), systemImage: "pencil")
        }
        .wandrGlassButton()
    }

    private func stat(label: LocalizedStringKey, value: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(value)
                .font(.title3.weight(.semibold))
                .foregroundStyle(Color.wandrAccentText)
                .fixedSize()
            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)
                .fixedSize()
        }
        .accessibilityElement(children: .combine)
    }
}

#Preview("Light Mode") {
    ProfileStatsRowView(memberSince: Date(timeIntervalSince1970: 1_768_435_200), activityCount: 42)
        .padding()
        .background(Color(.systemGroupedBackground))
        .preferredColorScheme(.light)
}

#Preview("Dark Mode, unknown date") {
    ProfileStatsRowView(activityCount: 0)
        .padding()
        .background(Color(.systemGroupedBackground))
        .preferredColorScheme(.dark)
}

#Preview("Large text") {
    ProfileStatsRowView(memberSince: Date(timeIntervalSince1970: 1_768_435_200), activityCount: 1234)
        .padding()
        .background(Color(.systemGroupedBackground))
        .dynamicTypeSize(.accessibility2)
}
