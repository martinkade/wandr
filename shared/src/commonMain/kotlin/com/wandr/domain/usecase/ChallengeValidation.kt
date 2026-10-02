package com.wandr.domain.usecase

import com.wandr.domain.model.Challenge

/** Shared create / update rules; returns the first violation or null. */
internal fun Challenge.validationError(): String? = when {
    title.isBlank() -> "Challenge title cannot be blank"
    targetValue <= 0.0 -> "Target value must be greater than 0"
    endDate <= startDate -> "End date must be after start date"
    else -> null
}
