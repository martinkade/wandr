package com.wandr.android.ui.social

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.wandr.domain.model.SocialEntityType
import com.wandr.presentation.social.SocialIntent
import com.wandr.presentation.social.SocialViewModel
import org.koin.compose.koinInject

/**
 * [SocialSection] wired to its own view model.
 *
 * @param entityOwnerId owner of the activity / challenge, who may also delete other users' comments on it
 */
@Composable
fun SocialSectionHost(
    type: SocialEntityType,
    entityId: String,
    userId: String,
    entityOwnerId: String?,
    modifier: Modifier = Modifier,
    viewModel: SocialViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(type, entityId, userId, entityOwnerId) {
        viewModel.processIntent(SocialIntent.Load(type, entityId, userId, entityOwnerId))
    }
    SocialSection(state = state, onIntent = viewModel::processIntent, modifier = modifier)
}
