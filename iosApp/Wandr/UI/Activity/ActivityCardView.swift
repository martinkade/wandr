import SwiftUI
@preconcurrency import shared

/// A feed card: who recorded the activity and when, its key figures, the route (if there is one) and likes / comments.
/// The upper part opens the details; the social bar below has its own buttons (no nested buttons).
struct ActivityCardView: View {
    let activity: Activity
    let authorName: String
    let authorAvatarUrl: String?
    var likeCount: Int = 0
    var likedByMe: Bool = false
    var commentCount: Int = 0
    /// The signed-in user recorded it: they always see the route and are told when others do not.
    var isOwn: Bool = false
    var onOpen: () -> Void = {}
    var onToggleLike: () -> Void = {}

    var body: some View {
        // Decoded once per body; a simplified polyline has a few hundred points at most.
        let route = activity.route
        VStack(alignment: .leading, spacing: 0) {
            Button(action: onOpen) {
                VStack(alignment: .leading, spacing: 12) {
                    ActivityOwnerView(
                        authorName: authorName, authorAvatarUrl: authorAvatarUrl,
                        activityType: activity.activityType, startTime: activity.startTime
                    )
                    VStack(alignment: .leading, spacing: 4) {
                        Text(activity.title)
                            .font(.title3.weight(.semibold))
                            .multilineTextAlignment(.leading)
                        if let description = activity.description_, !description.isEmpty {
                            Text(description)
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                                .lineLimit(2)
                                .multilineTextAlignment(.leading)
                        }
                    }
                    ActivityStatsView(
                        activityType: activity.activityType, distanceMeters: activity.distanceMeters,
                        durationSeconds: activity.durationSeconds, elevationGainMeters: activity.elevationGainMeters
                    )
                    if route.count >= 2 {
                        TrackMapView(trackpoints: route, height: 180, cornerRadius: 14)
                        if isOwn && !activity.showMap {
                            Label(LocalizedStringKey("activity_map_hidden_hint"), systemImage: "eye.slash")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            SocialBarView(
                likeCount: likeCount, likedByMe: likedByMe, commentCount: commentCount,
                onToggleLike: onToggleLike, onOpenComments: onOpen
            )
            .padding(.horizontal, 16)
            .padding(.bottom, 16)
        }
        .wandrCard()
    }
}

#Preview("Light Mode") {
    ScrollView {
        VStack(spacing: 16) {
            ActivityCardView(activity: .sample(), authorName: "Kristian Blummenfelt", authorAvatarUrl: nil, likeCount: 1_800, likedByMe: true, commentCount: 42)
            ActivityCardView(activity: .sample(polyline: nil, isManualEntry: true), authorName: "", authorAvatarUrl: nil)
        }
        .padding()
    }
    .background(Color(.systemGroupedBackground))
    .preferredColorScheme(.light)
}

#Preview("Dark Mode, own, map hidden") {
    ActivityCardView(activity: .sample(type: "cycling", showMap: false), authorName: "Me", authorAvatarUrl: nil, isOwn: true)
        .padding()
        .background(Color(.systemGroupedBackground))
        .preferredColorScheme(.dark)
}

#Preview("Large Text") {
    ScrollView {
        ActivityCardView(activity: .sample(), authorName: "Sophie", authorAvatarUrl: nil, likeCount: 3, commentCount: 1).padding()
    }
    .background(Color(.systemGroupedBackground))
    .environment(\.dynamicTypeSize, .accessibility2)
}
