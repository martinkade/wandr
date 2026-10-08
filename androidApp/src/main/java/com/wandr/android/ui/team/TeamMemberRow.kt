package com.wandr.android.ui.team

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.common.AvatarImage
import com.wandr.android.ui.common.sectionItemShape
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.model.TeamMember
import com.wandr.domain.model.TeamRole

/**
 * A member as a card of a list of connected cards, like the teams in the profile: [isFirstItemInSection] /
 * [isLastItemInSection] round the corners at the ends of the list.
 */
@Composable
fun TeamMemberRow(
    member: TeamMember,
    isFirstItemInSection: Boolean,
    isLastItemInSection: Boolean,
    modifier: Modifier = Modifier
) {
    val name = member.displayName.ifBlank { member.username }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = CardDefaults.cardColors().containerColor,
                shape = sectionItemShape(isFirstItemInSection, isLastItemInSection)
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AvatarImage(
            avatarUrl = member.avatarUrl,
            displayName = name,
            size = 40.dp
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (member.username.isNotBlank()) {
                Text(
                    text = "@${member.username}",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (member.role != TeamRole.MEMBER) {
            RoleChip(role = member.role)
        }
    }
}

@Composable
private fun RoleChip(role: TeamRole) {
    val isAdmin = role == TeamRole.ADMIN
    Text(
        text = stringResource(if (isAdmin) R.string.role_admin else R.string.role_member),
        style = MaterialTheme.typography.labelMedium,
        color = if (isAdmin) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (isAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Composable
private fun TeamMemberRowPreview() {
    WandrTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            TeamMemberRow(
                TeamMember("1", "t1", "u1", TeamRole.ADMIN, "martinkade", "Martin Kade", null, 0L),
                isFirstItemInSection = true, isLastItemInSection = false
            )
            TeamMemberRow(
                TeamMember("2", "t1", "u2", TeamRole.MEMBER, "runner99", "Alex Runner", null, 0L),
                isFirstItemInSection = false, isLastItemInSection = true
            )
        }
    }
}
