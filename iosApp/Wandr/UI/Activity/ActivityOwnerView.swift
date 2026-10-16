import SwiftUI
@preconcurrency import shared

/// Who recorded the activity: avatar, name and "date · type".
struct ActivityOwnerView: View {
    let authorName: String
    let authorAvatarUrl: String?
    let activityType: String
    /// Epoch milliseconds.
    let startTime: Int64

    private var name: String { authorName.isEmpty ? String(localized: "social_unknown_author") : authorName }

    var body: some View {
        HStack(spacing: 12) {
            AvatarView(url: authorAvatarUrl, name: name, size: 40)
            VStack(alignment: .leading, spacing: 2) {
                Text(name)
                    .font(.headline)
                    .lineLimit(1)
                Text("\(ActivityFormat.date(millis: startTime).formatted(date: .abbreviated, time: .shortened)) · \(ActivityFormat.typeName(activityType))")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            Spacer(minLength: 0)
        }
        .accessibilityElement(children: .combine)
    }
}

#Preview("Light Mode") {
    ActivityOwnerView(authorName: "Kristian Blummenfelt", authorAvatarUrl: nil, activityType: "running", startTime: 1_768_435_200_000)
        .padding().preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    ActivityOwnerView(authorName: "", authorAvatarUrl: nil, activityType: "cycling", startTime: 1_768_435_200_000)
        .padding().preferredColorScheme(.dark)
}

#Preview("Large Text") {
    ActivityOwnerView(authorName: "Sophie", authorAvatarUrl: nil, activityType: "hiking", startTime: 1_768_435_200_000)
        .padding().environment(\.dynamicTypeSize, .accessibility2)
}
