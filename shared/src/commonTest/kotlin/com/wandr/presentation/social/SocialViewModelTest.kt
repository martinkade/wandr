package com.wandr.presentation.social

import com.wandr.domain.model.Comment
import com.wandr.domain.model.ReactionSummary
import com.wandr.domain.model.SocialEntityType
import com.wandr.domain.model.SocialSummary
import com.wandr.domain.repository.SocialRepository
import com.wandr.domain.usecase.AddCommentUseCase
import com.wandr.domain.usecase.DeleteCommentUseCase
import com.wandr.domain.usecase.GetCommentsUseCase
import com.wandr.domain.usecase.GetSocialSummaryUseCase
import com.wandr.domain.usecase.SetLikeUseCase
import com.wandr.domain.usecase.SetReactionUseCase
import com.wandr.domain.usecase.UpdateCommentUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class FakeSocialRepository(var comments: MutableList<Comment> = mutableListOf()) : SocialRepository {
    var likes = 2
    var likedByMe = false
    var failNext = false
    val calls = mutableListOf<String>()

    private fun <T> result(value: T): Result<T> =
        if (failNext) { failNext = false; Result.failure(IllegalStateException("offline")) } else Result.success(value)

    override suspend fun getSummary(type: SocialEntityType, entityId: String, userId: String) = result(SocialSummary(likes, likedByMe))
    override suspend fun getCounts(type: SocialEntityType, entityIds: List<String>): Result<Map<String, com.wandr.domain.model.SocialCounts>> =
        result(entityIds.associateWith { com.wandr.domain.model.SocialCounts() })
    override suspend fun setLike(type: SocialEntityType, entityId: String, userId: String, liked: Boolean): Result<Unit> {
        calls += "like:$liked"
        return result(Unit)
    }
    override suspend fun getComments(type: SocialEntityType, entityId: String, userId: String) = result(comments.toList())
    override suspend fun addComment(type: SocialEntityType, entityId: String, userId: String, content: String): Result<Comment> {
        calls += "add:$content"
        return result(comment("new", userId, content))
    }
    override suspend fun updateComment(commentId: String, content: String): Result<Comment> {
        calls += "update:$commentId"
        return result(comment(commentId, "me", content).copy(updatedAt = 99_999))
    }
    override suspend fun deleteComment(commentId: String): Result<Unit> {
        calls += "delete:$commentId"
        return result(Unit)
    }
    override suspend fun setReaction(commentId: String, userId: String, emoji: String, reacted: Boolean): Result<Unit> {
        calls += "react:$emoji:$reacted"
        return result(Unit)
    }
}

private fun comment(id: String, userId: String, content: String = "hi") = Comment(
    id = id, entityType = SocialEntityType.ACTIVITY, entityId = "a1", userId = userId, authorName = userId,
    authorAvatarUrl = null, content = content, createdAt = 0L, updatedAt = 0L
)

@OptIn(ExperimentalCoroutinesApi::class)
class SocialViewModelTest {
    private fun kotlinx.coroutines.test.TestScope.vm(repo: FakeSocialRepository, owner: String? = "owner"): SocialViewModel {
        val viewModel = SocialViewModel(
            GetSocialSummaryUseCase(repo), SetLikeUseCase(repo), GetCommentsUseCase(repo), AddCommentUseCase(repo),
            UpdateCommentUseCase(repo), DeleteCommentUseCase(repo), SetReactionUseCase(repo),
            CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        )
        viewModel.processIntent(SocialIntent.Load(SocialEntityType.ACTIVITY, "a1", "me", owner))
        return viewModel
    }

    @Test
    fun loadsSummaryAndComments() = runTest {
        val viewModel = vm(FakeSocialRepository(mutableListOf(comment("c1", "x"))))
        assertEquals(2, viewModel.state.value.summary.likeCount)
        assertEquals(listOf("c1"), viewModel.state.value.comments.map { it.id })
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun likeIsOptimisticAndRevertedOnFailure() = runTest {
        val repo = FakeSocialRepository()
        val viewModel = vm(repo)
        viewModel.processIntent(SocialIntent.ToggleLike)
        assertEquals(SocialSummary(3, true), viewModel.state.value.summary)

        repo.failNext = true
        viewModel.processIntent(SocialIntent.ToggleLike)
        assertEquals(SocialSummary(3, true), viewModel.state.value.summary) // un-like failed -> reverted
        assertIs<com.wandr.domain.error.AppError.Unknown>(viewModel.state.value.error)
    }

    @Test
    fun postedCommentIsAppendedAndInvalidOnesAreRejectedWithoutACall() = runTest {
        val repo = FakeSocialRepository()
        val viewModel = vm(repo)
        viewModel.processIntent(SocialIntent.PostComment("  Nice!  "))
        assertEquals(listOf("add:Nice!"), repo.calls)
        assertEquals(1, viewModel.state.value.comments.size)

        viewModel.processIntent(SocialIntent.PostComment("   "))
        assertEquals(
            com.wandr.domain.error.InputProblem.COMMENT_EMPTY,
            assertIs<com.wandr.domain.error.AppError.InvalidInput>(viewModel.state.value.error).problem
        )
        viewModel.processIntent(SocialIntent.PostComment("x".repeat(1001)))
        assertEquals(
            com.wandr.domain.error.InputProblem.COMMENT_TOO_LONG,
            assertIs<com.wandr.domain.error.AppError.InvalidInput>(viewModel.state.value.error).problem
        )
        assertEquals(1, repo.calls.size)
    }

    @Test
    fun onlyTheAuthorEditsAndAuthorOrEntityOwnerDeletes() = runTest {
        val repo = FakeSocialRepository(mutableListOf(comment("mine", "me"), comment("other", "x")))
        val viewModel = vm(repo, owner = "me") // the signed-in user owns the activity

        viewModel.processIntent(SocialIntent.UpdateComment("other", "changed"))
        assertFalse(repo.calls.any { it.startsWith("update") })
        viewModel.processIntent(SocialIntent.UpdateComment("mine", "changed"))
        assertTrue(repo.calls.contains("update:mine"))

        viewModel.processIntent(SocialIntent.DeleteComment("other")) // allowed: owner of the activity
        assertEquals(listOf("mine"), viewModel.state.value.comments.map { it.id })
    }

    @Test
    fun strangersCannotDeleteOthersComments() = runTest {
        val repo = FakeSocialRepository(mutableListOf(comment("other", "x")))
        val viewModel = vm(repo, owner = "owner")
        viewModel.processIntent(SocialIntent.DeleteComment("other"))
        assertTrue(repo.calls.isEmpty())
        assertEquals(1, viewModel.state.value.comments.size)
    }

    @Test
    fun reactionToggleUpdatesCountsAndReverts() = runTest {
        val repo = FakeSocialRepository(mutableListOf(comment("c1", "x")))
        val viewModel = vm(repo)
        viewModel.processIntent(SocialIntent.ToggleReaction("c1", "👍"))
        assertEquals(listOf(ReactionSummary("👍", 1, true)), viewModel.state.value.comments.single().reactions)

        viewModel.processIntent(SocialIntent.ToggleReaction("c1", "👍"))
        assertTrue(viewModel.state.value.comments.single().reactions.isEmpty())

        repo.failNext = true
        viewModel.processIntent(SocialIntent.ToggleReaction("c1", "🔥"))
        assertTrue(viewModel.state.value.comments.single().reactions.isEmpty())
        assertIs<com.wandr.domain.error.AppError.Unknown>(viewModel.state.value.error)
    }

    @Test
    fun unsupportedReactionIsRejected() = runTest {
        val repo = FakeSocialRepository(mutableListOf(comment("c1", "x")))
        val viewModel = vm(repo)
        viewModel.processIntent(SocialIntent.ToggleReaction("c1", "💩"))
        assertTrue(repo.calls.isEmpty())
        assertTrue(viewModel.state.value.comments.single().reactions.isEmpty())
    }
}
