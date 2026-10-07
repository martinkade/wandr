package com.wandr.data.sync

import com.wandr.data.local.dao.ActivityDao
import com.wandr.data.local.dao.ChallengeDao
import com.wandr.data.local.dao.ProfileDao
import com.wandr.data.local.dao.TeamDao
import com.wandr.data.remote.ActivityRouteDto
import com.wandr.data.remote.SupabaseErrorMapper
import com.wandr.data.remote.toDto
import com.wandr.data.remote.toUpdatePayload
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class SyncManager(
    private val profileDao: ProfileDao,
    private val teamDao: TeamDao,
    private val challengeDao: ChallengeDao,
    private val activityDao: ActivityDao,
    private val supabase: SupabaseClient,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    /** A change arrived while a sync was running; that run may have missed it, so another one follows. */
    private var resyncRequested = false

    fun triggerSync() {
        if (_syncState.value is SyncState.Syncing) {
            resyncRequested = true
            return
        }
        _syncState.value = SyncState.Syncing

        scope.launch {
            try {
                var totalSynced = 0

                // 1. Sync Profiles
                val dirtyProfiles = profileDao.getDirtyProfiles()
                for (profile in dirtyProfiles) {
                    supabase.postgrest.from("profiles").update(profile.toUpdatePayload()) {
                        filter { eq("id", profile.id) }
                    }
                    profileDao.insertProfile(profile.copy(syncStatus = "SYNCED"))
                    totalSynced++
                }

                // 2. Sync Teams
                val dirtyTeams = teamDao.getDirtyTeams()
                for (team in dirtyTeams) {
                    supabase.postgrest.from("teams").update(team.toUpdatePayload()) {
                        filter { eq("id", team.id) }
                    }
                    teamDao.insertTeam(team.copy(syncStatus = "SYNCED"))
                    totalSynced++
                }

                // 3. Sync Challenges
                val dirtyChallenges = challengeDao.getDirtyChallenges()
                for (challenge in dirtyChallenges) {
                    supabase.postgrest.from("challenges").update(challenge.toUpdatePayload()) {
                        filter { eq("id", challenge.id) }
                    }
                    challengeDao.insertChallenge(challenge.copy(syncStatus = "SYNCED"))
                    totalSynced++
                }

                // 4. Sync Activities
                val dirtyActivities = activityDao.getDirtyActivities()
                for (activity in dirtyActivities) {
                    supabase.postgrest.from("activities").upsert(activity.toDto()) // never includes the local FIT path
                    // The route (simplified polyline) lives in its own table so the server can hide it per activity.
                    activity.polyline?.let {
                        supabase.postgrest.from("activity_routes").upsert(ActivityRouteDto(activity.id, activity.userId, it))
                    }
                    activityDao.insertActivity(activity.copy(syncStatus = "SYNCED"))
                    totalSynced++
                }

                // 5. Deleted activities: delete on the server, then forget the row. (The route and, via a trigger, the
                // likes and comments of the activity go with it.)
                for (activity in activityDao.getDeletedActivities()) {
                    supabase.postgrest.from("activities").delete { filter { eq("id", activity.id) } }
                    activityDao.deleteActivity(activity.id)
                    totalSynced++
                }

                _syncState.value = SyncState.Success(syncedCount = totalSynced)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(SupabaseErrorMapper.map(e))
            }
            if (resyncRequested) {
                resyncRequested = false
                triggerSync()
            }
        }
    }
}
