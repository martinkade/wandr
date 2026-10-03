package com.wandr.android.ui.profile.components

import android.content.res.Configuration
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.wandr.android.R
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.model.Team

/**
 * The groups of the user, ordered by priority, with a trailing item to join another one. A group is moved by dragging
 * its handle; [onReorder] reports the new order when the finger is lifted. The first group is the only one that counts
 * for group challenges.
 *
 * @param teams the saved order; when it changes (e.g. a rejected reorder is reverted) the list follows it
 */
@Composable
fun TeamMembershipList(
    teams: List<Team>,
    onReorder: (List<Team>) -> Unit,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var order by remember(teams) { mutableStateOf(teams) }
    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val heights = remember { mutableStateMapOf<String, Int>() }
    val currentOrder by rememberUpdatedState(order)
    val currentTeams by rememberUpdatedState(teams)

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = stringResource(R.string.memberships_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        if (teams.size > 1) {
            Text(
                text = stringResource(R.string.memberships_hint),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 6.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        order.forEachIndexed { index, team ->
            // The key keeps the drag gesture alive while the item changes its position in the list.
            key(team.id) {
                val isDragged = team.id == draggedId
                TeamMembershipItem(
                    team = team,
                    isPrimary = index == 0,
                    isFirstItemInSection = index == 0,
                    isLastItemInSection = false,
                    modifier = Modifier
                        .onSizeChanged { heights[team.id] = it.height }
                        .zIndex(if (isDragged) 1f else 0f)
                        .graphicsLayer { translationY = if (isDragged) dragOffset else 0f }
                        .shadow(if (isDragged) 8.dp else 0.dp),
                    dragHandleModifier = Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                draggedId = team.id
                                dragOffset = 0f
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffset += amount.y
                                val height = (heights[team.id] ?: 0).toFloat()
                                val from = currentOrder.indexOfFirst { it.id == team.id }
                                // Swap with the neighbour once the item is dragged over half of it.
                                if (height > 0 && dragOffset > height / 2 && from < currentOrder.lastIndex) {
                                    order = currentOrder.moved(from, from + 1)
                                    dragOffset -= height
                                } else if (height > 0 && dragOffset < -height / 2 && from > 0) {
                                    order = currentOrder.moved(from, from - 1)
                                    dragOffset += height
                                }
                            },
                            onDragEnd = {
                                draggedId = null
                                dragOffset = 0f
                                if (currentOrder.map { it.id } != currentTeams.map { it.id }) onReorder(
                                    currentOrder
                                )
                            },
                            onDragCancel = {
                                draggedId = null
                                dragOffset = 0f
                                order = currentTeams
                            }
                        )
                    }
                )
            }
        }

        JoinTeamItem(
            onClick = onJoin,
            isFirstItemInSection = teams.isEmpty(),
            isLastItemInSection = true
        )
    }
}

private val previewTeams = listOf(
    Team("t1", "Alpine Trail Blazers", null, null, null, "X7K9P2W1", "u1", 0L, 0L),
    Team("t2", "City Runners", null, null, null, "Q3M8D5LA", "u1", 0L, 0L),
    Team("t3", "Weekend Cyclists", null, null, null, "A1B2C3D4", "u1", 0L, 0L)
)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true, heightDp = 900)
@Preview(name = "Tablet", widthDp = 840, heightDp = 600, showBackground = true)
@Composable
private fun TeamMembershipListPreview() {
    WandrTheme {
        Surface {
            TeamMembershipList(
                previewTeams,
                onReorder = {},
                onJoin = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "No group yet", showBackground = true)
@Composable
private fun TeamMembershipListEmptyPreview() {
    WandrTheme {
        Surface {
            TeamMembershipList(
                emptyList(),
                onReorder = {},
                onJoin = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
