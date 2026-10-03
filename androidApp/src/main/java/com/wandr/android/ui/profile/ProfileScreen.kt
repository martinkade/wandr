package com.wandr.android.ui.profile

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.common.LabeledLoading
import com.wandr.android.ui.common.ScreenScaffold
import com.wandr.android.ui.profile.components.HealthImportCard
import com.wandr.android.ui.profile.components.HealthStatsCard
import com.wandr.android.ui.profile.components.ProfileHeaderRow
import com.wandr.android.ui.profile.components.TeamMembershipsSection
import com.wandr.android.ui.profile.components.ProfileStatsRow
import com.wandr.android.util.AppDateFormatter
import com.wandr.presentation.profile.ProfileIntent
import com.wandr.presentation.profile.ProfileState
import com.wandr.presentation.profile.ProfileSuccess
import com.wandr.presentation.profile.ProfileViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun ProfileScreen(
    userId: String,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinInject()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(userId) { viewModel.processIntent(ProfileIntent.LoadProfile(userId)) }
    ProfileScreenContent(
        state = state,
        onIntent = viewModel::processIntent,
        onLogout = onLogout,
        modifier = modifier
    )
}

/**
 * Read-only profile. "Edit" opens [ProfileEditScreen] in a bottom sheet; saving closes the sheet.
 * Results (saved / photo updated / error) are reported in a snackbar (on the sheet while it is open).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileScreenContent(
    state: ProfileState,
    onIntent: (ProfileIntent) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by rememberSaveable { mutableStateOf(false) }
    // Only a save started from the sheet may close it (not, e.g., a photo update).
    var saveRequested by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val isSaving by rememberUpdatedState(state.isSaving)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { !isSaving } // no swipe-away while saving
    )

    fun closeSheet() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { isEditing = false }
    }

    val savedText = stringResource(R.string.profile_saved_message)
    val avatarText = stringResource(R.string.profile_avatar_updated_message)
    LaunchedEffect(state.success, state.errorMessage) {
        val message = when {
            state.errorMessage != null -> state.errorMessage
            state.success == ProfileSuccess.PROFILE_SAVED -> savedText
            state.success == ProfileSuccess.AVATAR_UPDATED -> avatarText
            else -> null
        } ?: return@LaunchedEffect

        if (state.success == ProfileSuccess.PROFILE_SAVED && saveRequested) closeSheet()
        saveRequested = false
        // Own scope: clearing the message changes the effect keys, which would cancel (and hide) the snackbar.
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
        onIntent(ProfileIntent.ClearMessages)
    }

    ScreenScaffold(
        title = stringResource(R.string.user_profile_title),
        modifier = modifier,
        actions = {
            Button(onClick = onLogout) { Text(stringResource(R.string.logout_button)) }
        },
        // While the sheet is open, its own host shows the messages.
        snackbarHost = { if (!isEditing) SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val profile = state.profile
            if (state.isLoading || profile == null) {
                LabeledLoading(
                    isLoading = true,
                    title = stringResource(R.string.init_loading),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // The photo is changed right here (tap), not in the edit sheet.
                    ProfileHeaderRow(
                        profile = profile,
                        onAvatarReady = { jpeg ->
                            onIntent(
                                ProfileIntent.UploadAvatar(
                                    profile.id,
                                    jpeg
                                )
                            )
                        },
                        onRemoveAvatar = { onIntent(ProfileIntent.RemoveAvatar(profile.id)) },
                        isAvatarBusy = state.isAvatarUpdating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )

                    // Re-format when the system language changes; a timestamp <= 0 means "unknown".
                    val locale = LocalConfiguration.current.locales[0]
                    val memberSince = remember(profile.createdAt, locale) {
                        profile.createdAt.takeIf { it > 0 }
                            ?.let { AppDateFormatter.formatDate(it, locale = locale) }
                    }
                    ProfileStatsRow(
                        activityCount = state.activityCount,
                        memberSince = memberSince,
                        onEnterEditMode = { isEditing = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .padding(vertical = 16.dp)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        HealthImportCard(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 8.dp)
                                .aspectRatio(1.25f)
                        )
                        
                        // Only appears once Health Connect is connected.
                        HealthStatsCard(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 8.dp)
                                .aspectRatio(1.25f)
                        )
                    }

                    TeamMembershipsSection(
                        userId = profile.id,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 16.dp)
                    )
                }
            }
        }
    }

    if (isEditing) {
        ModalBottomSheet(
            onDismissRequest = {
                onIntent(ProfileIntent.DiscardChanges)
                isEditing = false
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            ProfileEditScreen(
                state = state,
                onIntent = onIntent,
                onSave = {
                    saveRequested = true
                    onIntent(ProfileIntent.SaveProfile)
                },
                onCancel = {
                    onIntent(ProfileIntent.DiscardChanges)
                    closeSheet()
                },
                snackbarHostState = snackbarHostState
            )
        }
    }
}
