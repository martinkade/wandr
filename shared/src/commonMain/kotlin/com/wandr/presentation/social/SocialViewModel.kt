package com.wandr.presentation.social

import com.wandr.domain.model.Comment
import com.wandr.domain.model.ReactionSummary
import com.wandr.domain.model.SocialSummary
import com.wandr.domain.usecase.AddCommentUseCase
import com.wandr.domain.usecase.DeleteCommentUseCase
import com.wandr.domain.usecase.GetCommentsUseCase
import com.wandr.domain.usecase.GetSocialSummaryUseCase
import com.wandr.domain.usecase.SetLikeUseCase
import com.wandr.domain.usecase.SetReactionUseCase
import com.wandr.domain.usecase.UpdateCommentUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Likes, comments and reactions of ONE activity or challenge. Likes and reactions update optimistically. */
class SocialViewModel(
    private val getSummary: GetSocialSummaryUseCase,
    private val setLike: SetLikeUseCase,
    private val getComments: GetCommentsUseCase,
    private val addComment: AddCommentUseCase,
    private val updateComment: UpdateCommentUseCase,
    private val deleteComment: DeleteCommentUseCase,
    private val setReaction: SetReactionUseCase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    private val _state = MutableStateFlow(SocialState())
    val state: StateFlow<SocialState> = _state.asStateFlow()

    fun processIntent(intent: SocialIntent) {
        when (intent) {
            is SocialIntent.Load -> {
                _state.update {
                    SocialState(
                        entityType = intent.type,
                        entityId = intent.entityId,
                        currentUserId = intent.userId,
                        entityOwnerId = intent.entityOwnerId
                    )
                }
                refresh()
            }
            is SocialIntent.Refresh -> refresh()
            is SocialIntent.ToggleLike -> toggleLike()
            is SocialIntent.PostComment -> post(intent.content)
            is SocialIntent.UpdateComment -> edit(intent.commentId, intent.content)
            is SocialIntent.DeleteComment -> remove(intent.commentId)
            is SocialIntent.ToggleReaction -> toggleReaction(intent.commentId, intent.emoji)
            is SocialIntent.ClearMessages -> _state.update { it.copy(errorMessage = null) }
        }
    }

    private fun refresh() {
        val s = _state.value
        val entityId = s.entityId ?: return
        val userId = s.currentUserId ?: return
        _state.update { it.copy(isLoading = true) }
        scope.launch {
            val summary = getSummary(s.entityType, entityId, userId)
            val comments = getComments(s.entityType, entityId, userId)
            _state.update {
                it.copy(
                    isLoading = false,
                    summary = summary.getOrDefault(it.summary),
                    comments = comments.getOrDefault(it.comments),
                    errorMessage = (summary.exceptionOrNull() ?: comments.exceptionOrNull())?.let(::message)
                )
            }
        }
    }

    private fun toggleLike() {
        val s = _state.value
        val entityId = s.entityId ?: return
        val userId = s.currentUserId ?: return
        val liked = !s.summary.likedByMe
        _state.update { it.copy(summary = it.summary.withLike(liked)) }
        scope.launch {
            setLike(s.entityType, entityId, userId, liked).onFailure { error ->
                _state.update { it.copy(summary = it.summary.withLike(!liked), errorMessage = message(error)) }
            }
        }
    }

    private fun SocialSummary.withLike(liked: Boolean) = SocialSummary(
        likeCount = (likeCount + if (liked) 1 else -1).coerceAtLeast(0),
        likedByMe = liked
    )

    private fun post(content: String) {
        val s = _state.value
        val entityId = s.entityId ?: return
        val userId = s.currentUserId ?: return
        _state.update { it.copy(isPosting = true, errorMessage = null) }
        scope.launch {
            addComment(s.entityType, entityId, userId, content)
                .onSuccess { comment -> _state.update { it.copy(isPosting = false, comments = it.comments + comment) } }
                .onFailure { error -> _state.update { it.copy(isPosting = false, errorMessage = message(error)) } }
        }
    }

    private fun edit(commentId: String, content: String) {
        val existing = _state.value.comments.firstOrNull { it.id == commentId } ?: return
        if (!_state.value.canEdit(existing)) return
        _state.update { it.copy(isPosting = true, errorMessage = null) }
        scope.launch {
            updateComment(commentId, content)
                .onSuccess { updated ->
                    _state.update { s ->
                        // The server answer has no reactions; keep the known ones.
                        s.copy(isPosting = false, comments = s.comments.map { if (it.id == commentId) updated.copy(reactions = it.reactions) else it })
                    }
                }
                .onFailure { error -> _state.update { it.copy(isPosting = false, errorMessage = message(error)) } }
        }
    }

    private fun remove(commentId: String) {
        val existing = _state.value.comments.firstOrNull { it.id == commentId } ?: return
        if (!_state.value.canDelete(existing)) return
        scope.launch {
            deleteComment(commentId)
                .onSuccess { _state.update { s -> s.copy(comments = s.comments.filterNot { it.id == commentId }) } }
                .onFailure { error -> _state.update { it.copy(errorMessage = message(error)) } }
        }
    }

    private fun toggleReaction(commentId: String, emoji: String) {
        val userId = _state.value.currentUserId ?: return
        val comment = _state.value.comments.firstOrNull { it.id == commentId } ?: return
        val reacted = comment.reactions.none { it.emoji == emoji && it.reactedByMe }
        _state.update { it.withReaction(commentId, emoji, reacted) }
        scope.launch {
            setReaction(commentId, userId, emoji, reacted).onFailure { error ->
                _state.update { it.withReaction(commentId, emoji, !reacted).copy(errorMessage = message(error)) }
            }
        }
    }

    private fun SocialState.withReaction(commentId: String, emoji: String, reacted: Boolean) = copy(
        comments = comments.map { comment ->
            if (comment.id != commentId) comment else comment.copy(reactions = comment.reactions.withReaction(emoji, reacted))
        }
    )

    private fun List<ReactionSummary>.withReaction(emoji: String, reacted: Boolean): List<ReactionSummary> {
        val current = firstOrNull { it.emoji == emoji }
        val count = ((current?.count ?: 0) + if (reacted) 1 else -1).coerceAtLeast(0)
        val others = filterNot { it.emoji == emoji }
        return if (count == 0) others else (others + ReactionSummary(emoji, count, reacted))
            .sortedBy { com.wandr.domain.model.Reactions.allowed.indexOf(it.emoji).let { i -> if (i < 0) Int.MAX_VALUE else i } }
    }

    private fun message(error: Throwable) = error.message ?: "Something went wrong"
}
