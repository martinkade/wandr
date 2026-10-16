import SwiftUI
@preconcurrency import shared

/// One comment: author with avatar and time, the text, its reactions and (author / owner) an overflow menu to edit or delete.
struct CommentRowView: View {
    let comment: Comment
    let allowedReactions: [String]
    let canEdit: Bool
    let canDelete: Bool
    var onToggleReaction: (String) -> Void = { _ in }
    var onEdit: () -> Void = {}
    var onDelete: () -> Void = {}

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            AvatarView(url: comment.authorAvatarUrl, name: comment.authorName, size: 36)
            VStack(alignment: .leading, spacing: 6) {
                HStack(spacing: 6) {
                    Text(comment.authorName.isEmpty ? String(localized: "social_unknown_author") : comment.authorName)
                        .font(.subheadline.weight(.semibold))
                        .lineLimit(1)
                    Text(Date(timeIntervalSince1970: Double(comment.createdAt) / 1000), style: .relative)
                        .font(.caption).foregroundStyle(.secondary)
                    if comment.isEdited {
                        Text(LocalizedStringKey("comment_edited")).font(.caption).foregroundStyle(.secondary)
                    }
                    Spacer(minLength: 0)
                    if canEdit || canDelete {
                        Menu {
                            if canEdit {
                                Button(action: onEdit) { Label(LocalizedStringKey("comment_edit"), systemImage: "pencil") }
                            }
                            if canDelete {
                                Button(role: .destructive, action: onDelete) { Label(LocalizedStringKey("comment_delete"), systemImage: "trash") }
                            }
                        } label: {
                            Image(systemName: "ellipsis").frame(width: 28, height: 28)
                        }
                        .accessibilityLabel(Text(LocalizedStringKey("more_options")))
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
                        Image(systemName: "face.smiling").font(.subheadline).foregroundStyle(Color.wandrAccentText)
                    }
                    .accessibilityLabel(Text(LocalizedStringKey("comment_add_reaction")))
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
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
