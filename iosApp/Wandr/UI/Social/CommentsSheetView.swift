import SwiftUI
@preconcurrency import shared

/// The comments of an activity or challenge in a sheet: the list with reactions, and the composer pinned to the bottom
/// (a glass bar above the keyboard).
struct CommentsSheetView: View {
    @ObservedObject var social: SocialObserver
    @Environment(\.dismiss) private var dismiss
    @State private var draft = ""
    @State private var editingId: String?
    @FocusState private var composerFocused: Bool

    private var trimmed: String { draft.trimmingCharacters(in: .whitespacesAndNewlines) }

    var body: some View {
        NavigationStack {
            List {
                if social.comments.isEmpty {
                    Text(LocalizedStringKey("comments_empty"))
                        .foregroundStyle(.secondary)
                        .listRowBackground(Color.clear)
                }
                ForEach(social.comments, id: \.id) { comment in
                    CommentRowView(
                        comment: comment,
                        allowedReactions: social.allowedReactions,
                        canEdit: social.canEdit(comment),
                        canDelete: social.canDelete(comment),
                        onToggleReaction: { social.toggleReaction(comment.id, $0) },
                        onEdit: {
                            editingId = comment.id
                            draft = comment.content
                            composerFocused = true
                        },
                        onDelete: { social.delete(comment.id) }
                    )
                }
                if let error = social.errorMessage {
                    Text(error).font(.footnote).foregroundStyle(.red).listRowBackground(Color.clear)
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle(Text(String.localizedStringWithFormat(NSLocalizedString("comments_count", comment: ""), social.comments.count)))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { dismiss() } label: { Image(systemName: "xmark") }
                        .accessibilityLabel(Text(LocalizedStringKey("close_button")))
                }
            }
            .safeAreaInset(edge: .bottom) { composer }
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
    }

    private var composer: some View {
        HStack(alignment: .bottom, spacing: 8) {
            if editingId != nil {
                Button { editingId = nil; draft = "" } label: { Image(systemName: "xmark.circle.fill") }
                    .foregroundStyle(.secondary)
                    .accessibilityLabel(Text(LocalizedStringKey("cancel_button")))
            }
            TextField(LocalizedStringKey("comment_hint"), text: $draft, axis: .vertical)
                .lineLimit(1...4)
                .focused($composerFocused)
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .wandrGlass(cornerRadius: 22)
            Button {
                if let id = editingId { social.update(id, trimmed) } else { social.post(trimmed) }
                editingId = nil
                draft = ""
            } label: {
                Image(systemName: editingId == nil ? "arrow.up" : "checkmark")
                    .font(.headline)
                    .frame(width: 40, height: 40)
            }
            .wandrProminentButton()
            .buttonBorderShape(.circle)
            .disabled(trimmed.isEmpty || social.isPosting)
            .accessibilityLabel(Text(LocalizedStringKey("comment_send")))
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
    }
}

/// Likes and the comments button of an activity or challenge, with its own social view model; the comments open in a sheet.
struct EntitySocialBar: View {
    let type: SocialEntityType
    let entityId: String
    let userId: String?
    let ownerId: String?
    @StateObject private var social = SocialObserver()
    @State private var showComments = false

    var body: some View {
        SocialBarView(
            likeCount: social.likeCount,
            likedByMe: social.likedByMe,
            commentCount: social.comments.count,
            onToggleLike: { social.toggleLike() },
            onOpenComments: { showComments = true }
        )
        .task(id: "\(entityId)|\(userId ?? "")|\(ownerId ?? "")") {
            if let userId { social.start(type: type, entityId: entityId, userId: userId, ownerId: ownerId) }
        }
        .sheet(isPresented: $showComments) { CommentsSheetView(social: social) }
    }
}

#Preview("Light Mode") {
    EntitySocialBar(type: .activity, entityId: "a1", userId: nil, ownerId: nil).padding().preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    EntitySocialBar(type: .challenge, entityId: "c1", userId: nil, ownerId: nil).padding().preferredColorScheme(.dark)
}
