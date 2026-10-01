package com.wandr.data.remote

import com.wandr.data.local.entity.ProfileEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage

class ProfileRemoteDataSource(private val supabase: SupabaseClient) {

    suspend fun fetch(userId: String): ProfileDto? =
        supabase.postgrest.from(TABLE).select {
            filter { eq("id", userId) }
        }.decodeSingleOrNull<ProfileDto>()

    suspend fun push(profile: ProfileEntity) {
        supabase.postgrest.from(TABLE).update(profile.toUpdatePayload()) {
            filter { eq("id", profile.id) }
        }
    }

    /** Uploads a new avatar under a unique name (avoids stale CDN/image caches) and returns its public URL. */
    suspend fun uploadAvatar(userId: String, jpegBytes: ByteArray, fileName: String): String {
        val bucket = supabase.storage.from(AVATAR_BUCKET)
        val path = "$userId/$fileName"
        bucket.upload(path, jpegBytes) { contentType = io.ktor.http.ContentType.Image.JPEG }
        return bucket.publicUrl(path)
    }

    /** Deletes every avatar object of [userId] except [keepFileName] (pass null to delete all). */
    suspend fun deleteAvatars(userId: String, keepFileName: String? = null) {
        val bucket = supabase.storage.from(AVATAR_BUCKET)
        val stale = bucket.list(userId).map { it.name }.filter { it != keepFileName }
        if (stale.isNotEmpty()) bucket.delete(stale.map { "$userId/$it" })
    }

    companion object {
        const val TABLE = "profiles"
        const val AVATAR_BUCKET = "avatars"
    }
}
