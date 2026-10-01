package com.wandr.presentation.profile

sealed interface ProfileIntent {
    data class LoadProfile(val userId: String) : ProfileIntent
    data class DisplayNameChanged(val name: String) : ProfileIntent
    data class BioChanged(val bio: String) : ProfileIntent
    data class UploadAvatar(val userId: String, val bytes: ByteArray, val fileName: String) : ProfileIntent
    data object SaveProfile : ProfileIntent
}
