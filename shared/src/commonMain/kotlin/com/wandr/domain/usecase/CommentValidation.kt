package com.wandr.domain.usecase

import com.wandr.domain.error.AppError
import com.wandr.domain.error.InputProblem

internal object CommentValidation {
    const val MAX_LENGTH = 1000

    /** Why [text] (already trimmed) cannot be posted, or null if it can. */
    fun error(text: String): AppError.InvalidInput? = when {
        text.isEmpty() -> AppError.InvalidInput(InputProblem.COMMENT_EMPTY)
        text.length > MAX_LENGTH -> AppError.InvalidInput(InputProblem.COMMENT_TOO_LONG, MAX_LENGTH)
        else -> null
    }
}
