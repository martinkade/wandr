package com.wandr.domain.model

data class Challenge(
    val id: String,
    val title: String,
    val description: String?,
    val coverUrl: String?,
    val scope: String, // group, individual
    val type: String, // distance, elevation, time
    val targetValue: Double,
    val requireAllMembersCompletion: Boolean = false,
    val startDate: Long,
    val endDate: Long,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
    /** Stored status: true = `active` (published), false = `draft` (only the creator sees it). */
    val isActive: Boolean = true,
    /** Activity types that count for the challenge (e.g. `running`); empty = every type counts. */
    val activityTypes: List<String> = emptyList()
)

/**
 * Whether [userId] may see this challenge in lists: published challenges are open to everybody, a draft only to the
 * manager who created it. The server enforces the same (a non-manager never gets drafts), but the local cache can still
 * hold a draft, e.g. from before the user's role changed, so lists filter with this too.
 */
fun Challenge.isVisibleTo(userId: String, isManager: Boolean): Boolean = isActive || (isManager && createdBy == userId)
