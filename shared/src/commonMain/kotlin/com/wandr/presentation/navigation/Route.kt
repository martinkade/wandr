package com.wandr.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey

@Serializable
data object LoginRoute : Route

@Serializable
data object RegisterRoute : Route

@Serializable
data object ProfileRoute : Route

@Serializable
data object ChallengeListRoute : Route

@Serializable
data object CreateChallengeRoute : Route

@Serializable
data class TeamDetailsRoute(val teamId: String = "") : Route

@Serializable
data object CreateTeamRoute : Route

@Serializable
data class MemberListRoute(val teamId: String = "") : Route

@Serializable
data class TeamInviteQRCodeRoute(val inviteCode: String = "") : Route

@Serializable
data object ActivityHistoryRoute : Route

@Serializable
data object ManualActivityEntryRoute : Route

@Serializable
data object LiveGpsTrackingRoute : Route
