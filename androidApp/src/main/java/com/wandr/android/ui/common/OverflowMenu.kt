package com.wandr.android.ui.common

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.theme.WandrTheme

/** One entry of an [OverflowMenu]; [isDestructive] colors it red (delete). */
data class OverflowMenuItem(
    val label: String,
    @DrawableRes val icon: Int,
    val onClick: () -> Unit,
    val isDestructive: Boolean = false
)

/**
 * The three-dots button of a top bar with its popup. Shows nothing without [items] (e.g. for users who may not edit).
 *
 * @param contentColor tint of the button, as handed to the actions of a `CollapsingHeaderScaffold`
 */
@Composable
fun OverflowMenu(
    items: List<OverflowMenuItem>,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    if (items.isEmpty()) return
    var open by rememberSaveable { mutableStateOf(false) }
    Box(modifier) {
        IconButton(onClick = { open = true }) {
            Icon(
                painterResource(R.drawable.ic_more_vert),
                contentDescription = stringResource(R.string.more_options),
                tint = contentColor
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            items.forEach { item ->
                val color =
                    if (item.isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                DropdownMenuItem(
                    text = { Text(item.label, color = color) },
                    leadingIcon = {
                        Icon(
                            painterResource(item.icon),
                            contentDescription = null,
                            tint = color
                        )
                    },
                    onClick = {
                        open = false
                        item.onClick()
                    }
                )
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun OverflowMenuPreview() {
    WandrTheme {
        Surface {
            OverflowMenu(
                items = listOf(
                    OverflowMenuItem("Edit", R.drawable.ic_edit, {}),
                    OverflowMenuItem("Change cover photo", R.drawable.ic_image, {}),
                    OverflowMenuItem("Delete", R.drawable.ic_delete, {}, isDestructive = true)
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
