import SwiftUI
@preconcurrency import shared

struct CommentRowView: View {
    let comment: Comment
    let allowedReactions: [String]
    let canEdit: Bool
    let canDelete: Bool
    var onToggleReaction: (String) -> Void = { _ in }
    var onEdit: () -> Void = {}
    var onDelete: () -> Void = {}

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 6) {
                Text(comment.authorName).font(.subheadline).fontWeight(.semibold)
                Text(Date(timeIntervalSince1970: Double(comment.createdAt) / 1000), style: .relative)
                    .font(.caption).foregroundColor(.secondary)
                if comment.isEdited {
                    Text(LocalizedStringKey("comment_edited")).font(.caption).foregroundColor(.secondary)
                }
            }
            Text(comment.content).font(.body)
            HStack(spacing: 6) {
                ForEach(comment.reactions, id: \.emoji) { reaction in
                    ReactionChipView(emoji: reaction.emoji, count: Int(reaction.count), isSelected: reaction.reactedByMe) {
                        onToggleReaction(reaction.emoji)
                    }
                }
                Menu {
                    ForEach(allowedReactions, id: \.self) { emoji in
                        Button(emoji) { onToggleReaction(emoji) }
                    }
                } label: {
                    Image(systemName: "face.smiling").font(.subheadline).foregroundColor(.wandrAccentText)
                }
                .accessibilityLabel(Text(LocalizedStringKey("comment_add_reaction")))
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 12))
        .contextMenu {
            if canEdit {
                Button { onEdit() } label: { Label(LocalizedStringKey("comment_edit"), systemImage: "pencil") }
            }
            if canDelete {
                Button(role: .destructive) { onDelete() } label: { Label(LocalizedStringKey("comment_delete"), systemImage: "trash") }
            }
        }
    }
}

extension Comment {
    static func sample(edited: Bool = false) -> Comment {
        Comment(
            id: "c1", entityType: .activity, entityId: "a1", userId: "u1", authorName: "Alex",
            authorAvatarUrl: nil, content: "Great hike!", createdAt: 1_700_000_000_000,
            updatedAt: edited ? 1_700_000_100_000 : 1_700_000_000_000,
            reactions: [ReactionSummary(emoji: "👍", count: 2, reactedByMe: true)]
        )
    }
}

#Preview("Light Mode") {
    CommentRowView(comment: .sample(edited: true), allowedReactions: ["👍", "🔥"], canEdit: true, canDelete: true)
        .padding().preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    CommentRowView(comment: .sample(), allowedReactions: ["👍", "🔥"], canEdit: false, canDelete: false)
        .padding().preferredColorScheme(.dark)
}
