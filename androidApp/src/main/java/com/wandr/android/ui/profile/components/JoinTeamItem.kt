package com.wandr.android.ui.profile.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.theme.WandrTheme

/** Trailing list item that starts joining another team. */
@Composable
fun JoinTeamItem(
    onClick: () -> Unit,
    isFirstItemInSection: Boolean,
    isLastItemInSection: Boolean, modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(
                color = CardDefaults.cardColors().containerColor,
                shape = RoundedCornerShape(
                    topStart = if (isFirstItemInSection) 16.dp else 4.dp,
                    topEnd = if (isFirstItemInSection) 16.dp else 4.dp,
                    bottomStart = if (isLastItemInSection) 16.dp else 4.dp,
                    bottomEnd = if (isLastItemInSection) 16.dp else 4.dp
                )
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_add),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier
                .size(40.dp)
                .padding(8.dp)
        )
        Text(
            text = stringResource(R.string.memberships_join_item),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Composable
private fun JoinTeamItemPreview() {
    WandrTheme {
        JoinTeamItem(
            onClick = {},
            isFirstItemInSection = true,
            isLastItemInSection = true
        )
    }
}
