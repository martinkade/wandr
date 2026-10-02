package com.wandr.android.ui.notifications

import android.content.res.Configuration
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wandr.android.R
import com.wandr.android.ui.theme.WandrTheme

/** Top bar button that opens the notifications; shows the number of unread ones. */
@Composable
fun NotificationBellAction(unreadCount: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        BadgedBox(badge = {
            if (unreadCount > 0) Badge { Text(if (unreadCount > 99) "99+" else unreadCount.toString()) }
        }) {
            Icon(painterResource(R.drawable.ic_notifications), contentDescription = stringResource(R.string.notifications_open))
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun NotificationBellActionPreview() {
    WandrTheme {
        androidx.compose.foundation.layout.Row {
            NotificationBellAction(unreadCount = 0, onClick = {})
            NotificationBellAction(unreadCount = 3, onClick = {})
            NotificationBellAction(unreadCount = 150, onClick = {})
        }
    }
}
