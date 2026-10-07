package com.wandr.android.ui.activity.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wandr.android.ui.activity.activityTypeText
import com.wandr.android.ui.common.AvatarImage
import com.wandr.android.util.AppDateFormatter
import com.wandr.domain.model.Activity

@Composable
fun ActivityOwner(
    author: String,
    authorAvatarUrl: String?,
    activity: Activity,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        AvatarImage(avatarUrl = authorAvatarUrl, displayName = author, size = 40.dp)
        Column(Modifier.weight(1f)) {
            Text(
                text = author,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${
                    AppDateFormatter.formatDate(
                        epochMillis = activity.startTime,
                        locale = locale
                    )
                } · ${activityTypeText(activity.activityType)}",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}