package com.wandr.data.repository

import com.wandr.data.remote.supabaseResult
import com.wandr.domain.model.NotificationPreferences
import com.wandr.domain.repository.NotificationPreferencesRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class NotificationPreferencesDto(
    @SerialName("user_id") val userId: String,
    @SerialName("activity_likes") val activityLikes: Boolean = true,
    @SerialName("activity_comments") val activityComments: Boolean = true,
    @SerialName("challenge_comments") val challengeComments: Boolean = true
)

/** `public.notification_preferences`: one row per user, read by the notification triggers (no row = all on). */
class NotificationPreferencesRepositoryImpl(private val supabase: SupabaseClient) :
    NotificationPreferencesRepository {

    override suspend fun get(userId: String): Result<NotificationPreferences> = supabaseResult {
        val row = supabase.postgrest.from(TABLE).select { filter { eq("user_id", userId) } }
            .decodeSingleOrNull<NotificationPreferencesDto>()
        row?.let {
            NotificationPreferences(
                it.activityLikes,
                it.activityComments,
                it.challengeComments
            )
        }
            ?: NotificationPreferences()
    }

    override suspend fun save(userId: String, preferences: NotificationPreferences): Result<Unit> =
        supabaseResult {
            supabase.postgrest.from(TABLE).upsert(
                NotificationPreferencesDto(
                    userId,
                    preferences.activityLikes,
                    preferences.activityComments,
                    preferences.challengeComments
                )
            )
            Unit
        }

    private companion object {
        const val TABLE = "notification_preferences"
    }
}
