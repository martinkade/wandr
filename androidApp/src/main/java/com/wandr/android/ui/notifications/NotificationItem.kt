package com.wandr.android.ui.notifications

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.common.AvatarImage
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.android.util.AppDateFormatter
import com.wandr.domain.model.AppNotification
import com.wandr.domain.model.NotificationType
import com.wandr.domain.model.SocialEntityType

/** One notification row; unread ones are bold and marked with a dot. */
@Composable
fun NotificationItem(
    notification: AppNotification,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val actor = notification.actorName.ifBlank { stringResource(R.string.social_unknown_author) }
    val isChallenge = notification.entityType == SocialEntityType.CHALLENGE
    val text = when (notification.type) {
        NotificationType.LIKE -> stringResource(
            if (isChallenge) R.string.notification_like_challenge else R.string.notification_like_activity, actor
        )
        NotificationType.COMMENT -> stringResource(
            if (isChallenge) R.string.notification_comment_challenge else R.string.notification_comment_activity,
            actor, notification.preview.orEmpty()
        )
        NotificationType.REACTION -> stringResource(R.string.notification_reaction, actor, notification.emoji.orEmpty())
        else -> stringResource(R.string.notification_generic)
    }

    Row(
        modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarImage(avatarUrl = notification.actorAvatarUrl, displayName = actor, size = 40.dp)
        Column(Modifier.weight(1f)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold
            )
            Text(
                text = AppDateFormatter.formatDateTime(notification.createdAt, locale = locale),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!notification.isRead) {
            androidx.compose.foundation.layout.Box(
                Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

private val previewNotification = AppNotification(
    id = "n1", type = NotificationType.COMMENT, actorUserId = "u2", actorName = "Alex Kim", actorAvatarUrl = null,
    entityType = SocialEntityType.ACTIVITY, entityId = "a1", commentId = "c1", preview = "Great pace today!",
    emoji = null, isRead = false, createdAt = 1_768_435_200_000L
)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Composable
private fun NotificationItemPreview() {
    WandrTheme {
        Column {
            NotificationItem(previewNotification, onClick = {})
            NotificationItem(previewNotification.copy(type = NotificationType.REACTION, emoji = "🔥", isRead = true), onClick = {})
            NotificationItem(previewNotification.copy(type = NotificationType.LIKE, entityType = SocialEntityType.CHALLENGE), onClick = {})
        }
    }
}
