import SwiftUI
@preconcurrency import shared

/// Stateless likes + comments block for an activity (or challenge).
struct SocialSectionView: View {
    let likeCount: Int
    let likedByMe: Bool
    let comments: [Comment]
    let isPosting: Bool
    let errorMessage: String?
    var allowedReactions: [String] = []
    var canEdit: (Comment) -> Bool = { _ in false }
    var canDelete: (Comment) -> Bool = { _ in false }
    var onToggleLike: () -> Void = {}
    var onPost: (String) -> Void = { _ in }
    var onUpdate: (String, String) -> Void = { _, _ in }
    var onDelete: (String) -> Void = { _ in }
    var onToggleReaction: (String, String) -> Void = { _, _ in }

    @State private var draft = ""
    @State private var editingId: String?

    private var trimmed: String { draft.trimmingCharacters(in: .whitespacesAndNewlines) }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Button(action: onToggleLike) {
                Label("\(likeCount)", systemImage: likedByMe ? "heart.fill" : "heart")
                    .foregroundColor(.wandrAccentText)
            }
            .buttonStyle(.bordered)
            .accessibilityLabel(Text(LocalizedStringKey("like_button")))

            Text(LocalizedStringKey("comments_title")).font(.headline)

            if comments.isEmpty {
                Text(LocalizedStringKey("comments_empty")).font(.subheadline).foregroundColor(.secondary)
            }
            ForEach(comments, id: \.id) { comment in
                CommentRowView(
                    comment: comment,
                    allowedReactions: allowedReactions,
                    canEdit: canEdit(comment),
                    canDelete: canDelete(comment),
                    onToggleReaction: { onToggleReaction(comment.id, $0) },
                    onEdit: { editingId = comment.id; draft = comment.content },
                    onDelete: { onDelete(comment.id) }
                )
            }

            if let errorMessage {
                Text(errorMessage).font(.footnote).foregroundColor(.red)
            }

            HStack(alignment: .bottom, spacing: 8) {
                TextField(LocalizedStringKey("comment_hint"), text: $draft, axis: .vertical)
                    .lineLimit(1...4)
                    .textFieldStyle(.roundedBorder)
                if editingId != nil {
                    Button { editingId = nil; draft = "" } label: { Image(systemName: "xmark.circle") }
                        .accessibilityLabel(Text(LocalizedStringKey("cancel_button")))
                }
                Button {
                    if let id = editingId { onUpdate(id, trimmed) } else { onPost(trimmed) }
                    editingId = nil
                    draft = ""
                } label: {
                    Image(systemName: "paperplane.fill")
                }
                .disabled(trimmed.isEmpty || isPosting)
                .accessibilityLabel(Text(LocalizedStringKey("comment_send")))
            }
        }
    }
}

#Preview("Light Mode") {
    SocialSectionView(likeCount: 3, likedByMe: true, comments: [.sample(edited: true)], isPosting: false,
                      errorMessage: nil, allowedReactions: ["👍", "🔥"], canEdit: { _ in true }, canDelete: { _ in true })
        .padding().preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    SocialSectionView(likeCount: 0, likedByMe: false, comments: [], isPosting: false, errorMessage: "Something went wrong")
        .padding().preferredColorScheme(.dark)
}
