package com.wandr.domain.model

/** What a user can choose to be notified about (in the inbox and by push). Likes on challenges are never notified. */
enum class NotificationScope {
    /** Likes on my activities. */
    ACTIVITY_LIKES,

    /** Comments on my activities and reactions to my comments there. */
    ACTIVITY_COMMENTS,

    /** Comments on my challenges and reactions to my comments there. */
    CHALLENGE_COMMENTS
}

/** The user's choice per [NotificationScope]; everything is on until they change it. Stored on the server. */
data class NotificationPreferences(
    val activityLikes: Boolean = true,
    val activityComments: Boolean = true,
    val challengeComments: Boolean = true
) {
    fun isEnabled(scope: NotificationScope): Boolean = when (scope) {
        NotificationScope.ACTIVITY_LIKES -> activityLikes
        NotificationScope.ACTIVITY_COMMENTS -> activityComments
        NotificationScope.CHALLENGE_COMMENTS -> challengeComments
    }

    fun with(scope: NotificationScope, enabled: Boolean): NotificationPreferences = when (scope) {
        NotificationScope.ACTIVITY_LIKES -> copy(activityLikes = enabled)
        NotificationScope.ACTIVITY_COMMENTS -> copy(activityComments = enabled)
        NotificationScope.CHALLENGE_COMMENTS -> copy(challengeComments = enabled)
    }
}
