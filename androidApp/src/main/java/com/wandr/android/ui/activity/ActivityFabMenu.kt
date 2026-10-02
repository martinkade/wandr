package com.wandr.android.ui.activity

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.theme.WandrTheme

/** FAB that expands into two actions: record an activity with GPS, or log one manually. */
@Composable
fun ActivityFabMenu(
    onCreate: () -> Unit,
    onRecord: () -> Unit,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    BackHandler(enabled = expanded) { expanded = false }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExtendedFloatingActionButton(
                    onClick = {
                        expanded = false
                        onRecord()
                    },
                    icon = { Icon(painterResource(R.drawable.ic_record), contentDescription = null) },
                    text = { Text(stringResource(R.string.activity_fab_record)) }
                )
                ExtendedFloatingActionButton(
                    onClick = {
                        expanded = false
                        onCreate()
                    },
                    icon = { Icon(painterResource(R.drawable.ic_edit), contentDescription = null) },
                    text = { Text(stringResource(R.string.activity_fab_create)) }
                )
            }
        }
        FloatingActionButton(onClick = { expanded = !expanded }) {
            Icon(
                painter = painterResource(if (expanded) R.drawable.ic_close else R.drawable.ic_add),
                contentDescription = stringResource(if (expanded) R.string.activity_fab_close else R.string.activity_fab_open)
            )
        }
    }
}

@Preview(name = "Collapsed", showBackground = true)
@Preview(name = "Collapsed Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun ActivityFabMenuCollapsedPreview() {
    WandrTheme { ActivityFabMenu(onCreate = {}, onRecord = {}, modifier = Modifier.padding(16.dp)) }
}

@Preview(name = "Expanded", showBackground = true, heightDp = 300)
@Preview(name = "Expanded Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, heightDp = 300)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true, heightDp = 400)
@Composable
private fun ActivityFabMenuExpandedPreview() {
    WandrTheme { ActivityFabMenu(onCreate = {}, onRecord = {}, modifier = Modifier.padding(16.dp), initiallyExpanded = true) }
}
