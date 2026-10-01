package com.wandr.presentation.teamdetails

sealed interface TeamDetailsIntent {
    data class Load(val teamId: String, val userId: String) : TeamDetailsIntent
    data class NameChanged(val name: String) : TeamDetailsIntent
    data class DescriptionChanged(val description: String) : TeamDetailsIntent

    /** [jpegBytes] is the already cropped and scaled image (see AvatarImageSpec / CoverImageSpec). */
    data class UploadAvatar(val jpegBytes: ByteArray) : TeamDetailsIntent
    data class UploadCover(val jpegBytes: ByteArray) : TeamDetailsIntent
    data object RemoveAvatar : TeamDetailsIntent
    data object RemoveCover : TeamDetailsIntent
    data object Save : TeamDetailsIntent

    /** Drops unsaved name/description edits and shows the stored team again. */
    data object DiscardChanges : TeamDetailsIntent
    data object ClearMessages : TeamDetailsIntent
}
