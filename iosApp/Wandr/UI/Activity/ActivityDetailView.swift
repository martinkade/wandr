import SwiftUI
@preconcurrency import shared

/// Read-only activity details followed by likes and comments.
struct ActivityDetailView: View {
    let activity: Activity
    let userId: String?
    @StateObject private var social = SocialObserver()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 8) {
                    Text(activity.title).font(.title2).fontWeight(.bold)
                    Text(activity.activityType.capitalized).font(.subheadline).foregroundColor(.wandrAccentText)
                    Text(Date(timeIntervalSince1970: Double(activity.startTime) / 1000), format: .dateTime.day().month().year().hour().minute())
                        .font(.subheadline).foregroundColor(.secondary)
                    if let desc = activity.description_, !desc.isEmpty {
                        Text(desc).font(.body)
                    }
                    HStack {
                        Label(String(format: "%.2f km", activity.distanceMeters / 1000), systemImage: "ruler")
                            .frame(maxWidth: .infinity, alignment: .leading)
                        Label("\(Int(activity.durationSeconds / 60)) min", systemImage: "clock")
                            .frame(maxWidth: .infinity, alignment: .leading)
                        Label("\(Int(activity.elevationGainMeters)) m", systemImage: "mountain.2")
                    }
                    .font(.subheadline)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(16)
                .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16))

                SocialSectionView(
                    likeCount: social.likeCount, likedByMe: social.likedByMe, comments: social.comments,
                    isPosting: social.isPosting, errorMessage: social.errorMessage,
                    allowedReactions: social.allowedReactions,
                    canEdit: { social.canEdit($0) }, canDelete: { social.canDelete($0) },
                    onToggleLike: { social.toggleLike() }, onPost: { social.post($0) },
                    onUpdate: { social.update($0, $1) }, onDelete: { social.delete($0) },
                    onToggleReaction: { social.toggleReaction($0, $1) }
                )
            }
            .padding(16)
        }
        .navigationTitle(activity.title)
        .navigationBarTitleDisplayMode(.inline)
        .task(id: userId) {
            if let userId { social.start(entityId: activity.id, userId: userId, ownerId: activity.userId) }
        }
    }
}

extension Activity {
    static var sample: Activity {
        Activity(
            id: "a1", userId: "u1", teamId: "t1", title: "Weekend Trail Walk", description: "Lovely day.",
            activityType: "hiking", distanceMeters: 5400, durationSeconds: 3600, elevationGainMeters: 150,
            fitFilePath: nil, startTime: 1_700_000_000_000, endTime: 1_700_003_600_000,
            isManualEntry: true, createdAt: 0, updatedAt: 0
        )
    }
}

#Preview("Light Mode") { NavigationStack { ActivityDetailView(activity: .sample, userId: nil) }.preferredColorScheme(.light) }
#Preview("Dark Mode") { NavigationStack { ActivityDetailView(activity: .sample, userId: nil) }.preferredColorScheme(.dark) }
