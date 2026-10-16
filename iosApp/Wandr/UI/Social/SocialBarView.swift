import SwiftUI

/// Like button with its count and a comments button with the number of comments (glass chips); the comments open in a sheet.
struct SocialBarView: View {
    let likeCount: Int
    let likedByMe: Bool
    let commentCount: Int
    var onToggleLike: () -> Void = {}
    var onOpenComments: () -> Void = {}

    var body: some View {
        WandrGlassContainer(spacing: 8) {
            HStack(spacing: 8) {
                Button(action: onToggleLike) {
                    Label("\(likeCount)", systemImage: likedByMe ? "heart.fill" : "heart")
                        .foregroundStyle(likedByMe ? Color.red : Color.primary)
                        .contentTransition(.symbolEffect(.replace))
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                }
                .buttonStyle(.plain)
                .wandrGlassCapsule(interactive: true)
                .accessibilityLabel(Text(LocalizedStringKey("like_button")))
                .sensoryFeedback(.selection, trigger: likedByMe)

                Button(action: onOpenComments) {
                    Label("\(commentCount)", systemImage: "bubble.left")
                        .foregroundStyle(Color.primary)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                }
                .buttonStyle(.plain)
                .wandrGlassCapsule(interactive: true)
                .accessibilityLabel(Text(LocalizedStringKey("comments_title")))
            }
        }
        .font(.subheadline.weight(.semibold))
        .monospacedDigit()
    }
}

#Preview("Light Mode") {
    SocialBarView(likeCount: 12, likedByMe: true, commentCount: 4).padding().preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    SocialBarView(likeCount: 0, likedByMe: false, commentCount: 0).padding().preferredColorScheme(.dark)
}

#Preview("Large Text") {
    SocialBarView(likeCount: 1800, likedByMe: false, commentCount: 42).padding().environment(\.dynamicTypeSize, .accessibility2)
}
