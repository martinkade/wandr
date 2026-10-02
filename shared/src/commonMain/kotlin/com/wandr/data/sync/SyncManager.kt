package com.wandr.data.sync

import com.wandr.data.local.dao.ActivityDao
import com.wandr.data.local.dao.ChallengeDao
import com.wandr.data.local.dao.ProfileDao
import com.wandr.data.local.dao.TeamDao
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

    fun triggerSync() {
        if (_syncState.value is SyncState.Syncing) return
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
                    activityDao.insertActivity(activity.copy(syncStatus = "SYNCED"))
                    totalSynced++
                }

                _syncState.value = SyncState.Success(syncedCount = totalSynced)
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(message = e.message ?: "Sync failed")
            }
        }
    }
}
