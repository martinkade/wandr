package com.wandr.presentation.profile

sealed interface ProfileIntent {
    data class LoadProfile(val userId: String) : ProfileIntent
    data class DisplayNameChanged(val name: String) : ProfileIntent
    data class BioChanged(val bio: String) : ProfileIntent

    /** [jpegBytes] is the already cropped and scaled avatar (see [com.wandr.presentation.imagecrop.AvatarImageSpec]). */
    data class UploadAvatar(val userId: String, val jpegBytes: ByteArray) : ProfileIntent
    data class RemoveAvatar(val userId: String) : ProfileIntent
    data object SaveProfile : ProfileIntent
    data object ClearMessages : ProfileIntent
}
