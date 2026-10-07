package com.wandr.domain.usecase

import com.wandr.domain.error.AppError
import com.wandr.domain.error.InputProblem
import com.wandr.domain.model.Challenge

/** Shared create / update rules; returns the first violation or null. */
internal fun Challenge.validationError(): AppError.InvalidInput? = when {
    title.isBlank() -> AppError.InvalidInput(InputProblem.TITLE_REQUIRED)
    targetValue <= 0.0 -> AppError.InvalidInput(InputProblem.TARGET_NOT_POSITIVE)
    endDate <= startDate -> AppError.InvalidInput(InputProblem.END_BEFORE_START)
    else -> null
}

/** Trimmed title/description and a clean list of activity types (lower case, no blanks, no duplicates). */
internal fun Challenge.normalized(): Challenge = copy(
    title = title.trim(),
    description = description?.trim()?.ifEmpty { null },
    activityTypes = activityTypes.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.distinct()
)
