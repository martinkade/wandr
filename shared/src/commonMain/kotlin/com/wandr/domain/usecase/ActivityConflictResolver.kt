package com.wandr.domain.usecase

import com.wandr.domain.error.AppError
import com.wandr.domain.error.InputProblem
import com.wandr.domain.model.Activity
import com.wandr.domain.model.ActivityConflictException
import com.wandr.domain.model.ConflictResolution
import com.wandr.domain.model.GpsTrackpoint
import com.wandr.domain.repository.ActivityRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock

/**
 * Saves an activity while taking care of time overlaps with the user's other activities
 * (`startA < endB && endA > startB`, so activities that merely touch do not conflict).
 */
class ActivityConflictResolver(private val repository: ActivityRepository) {

    /** Other activities of the same user whose time range overlaps [candidate]'s, oldest first. */
    suspend fun findConflicts(candidate: Activity): List<Activity> =
        repository.getOverlappingActivities(candidate.userId, candidate.startTime, candidate.endTime)
            .filter { it.id != candidate.id }
            .sortedBy { it.startTime }

    /**
     * Saves [candidate]. Without a conflict it is saved as it is. With one, [resolution] decides; without a
     * resolution nothing is saved and an [ActivityConflictException] is returned. [ConflictResolution.DISCARD] means
     * "do not save" and is handled by the caller.
     */
    suspend fun save(
        candidate: Activity,
        trackpoints: List<GpsTrackpoint>?,
        resolution: ConflictResolution?
    ): Result<Activity> = try {
        val conflicts = findConflicts(candidate)
        Result.success(
            when {
                conflicts.isEmpty() -> repository.saveActivity(candidate, trackpoints).getOrThrow()
                resolution == null -> throw ActivityConflictException(
                    conflicts,
                    canTrim = freeRange(candidate, conflicts) != null
                )

                resolution == ConflictResolution.MERGE -> merge(candidate, trackpoints, conflicts)
                resolution == ConflictResolution.TRIM -> trim(candidate, trackpoints, conflicts)
                else -> throw IllegalArgumentException("Discarding does not save anything")
            }
        )
    } catch (cancelled: CancellationException) {
        throw cancelled // a cancelled coroutine has to stop, not become an error
    } catch (error: Throwable) {
        // The conflict exception is a signal for the caller (it opens the wizard) and must stay what it is.
        Result.failure(error)
    }

    private suspend fun merge(candidate: Activity, trackpoints: List<GpsTrackpoint>?, conflicts: List<Activity>): Activity {
        val all = conflicts + candidate
        val start = all.minOf { it.startTime }
        val end = all.maxOf { it.endTime }
        val merged = candidate.copy(
            distanceMeters = all.sumOf { it.distanceMeters },
            elevationGainMeters = all.sumOf { it.elevationGainMeters },
            // Every conflict overlaps the candidate, so together they cover one continuous range.
            durationSeconds = (end - start) / 1000.0,
            startTime = start,
            endTime = end,
            isManualEntry = all.all { it.isManualEntry },
            updatedAt = Clock.System.now().toEpochMilliseconds()
        )

        // Tracks only exist on the device that recorded them; whatever is available here is joined.
        val own = trackpoints ?: if (candidate.fitFilePath != null) repository.getTrackpoints(candidate.id) else emptyList()
        val track = (own + conflicts.flatMap { repository.getTrackpoints(it.id) }).sortedBy { it.timestamp }

        // Save first, delete afterwards: a failure never loses data.
        val saved = repository.saveActivity(merged, track.ifEmpty { null }).getOrThrow()
        conflicts.forEach { repository.deleteActivity(it.id).getOrThrow() }
        return saved
    }

    private suspend fun trim(candidate: Activity, trackpoints: List<GpsTrackpoint>?, conflicts: List<Activity>): Activity {
        val (start, end) = freeRange(candidate, conflicts)
            ?: throw AppError.InvalidInput(InputProblem.NOTHING_LEFT_TO_TRIM)
        val total = candidate.endTime - candidate.startTime
        // Measured values are shared out in proportion to the remaining time.
        val ratio = if (total > 0) (end - start).toDouble() / total else 1.0
        val trimmed = candidate.copy(
            distanceMeters = candidate.distanceMeters * ratio,
            durationSeconds = candidate.durationSeconds * ratio,
            elevationGainMeters = candidate.elevationGainMeters * ratio,
            startTime = start,
            endTime = end,
            updatedAt = Clock.System.now().toEpochMilliseconds()
        )
        val track = trackpoints?.filter { it.timestamp in start..end }
        return repository.saveActivity(trimmed, track?.ifEmpty { null }).getOrThrow()
    }

    /** The longest part of [candidate]'s time range that none of [conflicts] covers, or null if there is none. */
    internal fun freeRange(candidate: Activity, conflicts: List<Activity>): Pair<Long, Long>? {
        var best: Pair<Long, Long>? = null
        fun consider(from: Long, to: Long) {
            if (to > from && (best == null || to - from > best!!.second - best!!.first)) best = from to to
        }

        var cursor = candidate.startTime
        for (conflict in conflicts.sortedBy { it.startTime }) {
            if (conflict.startTime > cursor) consider(cursor, minOf(conflict.startTime, candidate.endTime))
            cursor = maxOf(cursor, conflict.endTime)
            if (cursor >= candidate.endTime) break
        }
        if (cursor < candidate.endTime) consider(cursor, candidate.endTime)
        return best
    }
}
