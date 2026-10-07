import SwiftUI
@preconcurrency import shared

@MainActor
final class SocialObserver: ObservableObject {
    @Published var likeCount = 0
    @Published var likedByMe = false
    @Published var comments: [Comment] = []
    @Published var isPosting = false
    @Published var errorMessage: String?

    private let viewModel = IosDependencies.shared.socialViewModel()
    nonisolated(unsafe) private var job: (any Kotlinx_coroutines_coreJob)?
    nonisolated(unsafe) private var latest: SocialState?
    let allowedReactions: [String] = Reactions.shared.allowed

    func start(entityId: String, userId: String, ownerId: String?) {
        job?.cancel(cause: nil)
        job = FlowObserverKt.watch(viewModel.state) { [weak self] value in
            guard let state = value as? SocialState else { return }
            Task { @MainActor in
                self?.latest = state
                self?.likeCount = Int(state.summary.likeCount)
                self?.likedByMe = state.summary.likedByMe
                self?.comments = state.comments
                self?.isPosting = state.isPosting
                self?.errorMessage = state.error?.userMessage
            }
        }
        viewModel.processIntent(intent: SocialIntentLoad(type: .activity, entityId: entityId, userId: userId, entityOwnerId: ownerId))
    }

    func canEdit(_ comment: Comment) -> Bool { latest?.canEdit(comment: comment) ?? false }
    func canDelete(_ comment: Comment) -> Bool { latest?.canDelete(comment: comment) ?? false }

    func toggleLike() { viewModel.processIntent(intent: SocialIntentToggleLike.shared) }
    func post(_ text: String) { viewModel.processIntent(intent: SocialIntentPostComment(content: text)) }
    func update(_ id: String, _ text: String) { viewModel.processIntent(intent: SocialIntentUpdateComment(commentId: id, content: text)) }
    func delete(_ id: String) { viewModel.processIntent(intent: SocialIntentDeleteComment(commentId: id)) }
    func toggleReaction(_ id: String, _ emoji: String) { viewModel.processIntent(intent: SocialIntentToggleReaction(commentId: id, emoji: emoji)) }

    deinit { job?.cancel(cause: nil) }
}
