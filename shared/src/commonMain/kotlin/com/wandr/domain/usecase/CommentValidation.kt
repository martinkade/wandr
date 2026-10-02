package com.wandr.domain.usecase

internal object CommentValidation {
    const val MAX_LENGTH = 1000

    /** Why [text] (already trimmed) cannot be posted, or null if it can. */
    fun error(text: String): String? = when {
        text.isEmpty() -> "Comment cannot be empty"
        text.length > MAX_LENGTH -> "Comment is too long (max. $MAX_LENGTH characters)"
        else -> null
    }
}
