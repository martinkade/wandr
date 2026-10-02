package com.wandr.di

import com.wandr.domain.usecase.CreateChallengeUseCase
import com.wandr.domain.usecase.CreateTeamUseCase
import com.wandr.domain.usecase.EvaluateChallengeStatusUseCase
import com.wandr.domain.usecase.GetChallengeLeaderboardUseCase
import com.wandr.domain.usecase.GetProfileUseCase
import com.wandr.domain.usecase.GetUserTeamsUseCase
import com.wandr.domain.usecase.JoinChallengeUseCase
import com.wandr.domain.usecase.JoinTeamViaInviteUseCase
import com.wandr.domain.usecase.LoginUseCase
import com.wandr.domain.usecase.LogoutUseCase
import com.wandr.domain.usecase.RegisterUseCase
import com.wandr.domain.usecase.UpdateProfileUseCase
import com.wandr.domain.usecase.UploadAvatarUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { com.wandr.domain.usecase.InitializeAppUseCase(get(), get()) }
    factory { LoginUseCase(get()) }
    factory { RegisterUseCase(get()) }
    factory { LogoutUseCase(get()) }
    factory { GetProfileUseCase(get()) }
    factory { UpdateProfileUseCase(get()) }
    factory { UploadAvatarUseCase(get()) }
    factory { com.wandr.domain.usecase.RemoveAvatarUseCase(get()) }
    factory { com.wandr.domain.usecase.RefreshProfileUseCase(get()) }
    factory { CreateTeamUseCase(get()) }
    factory { GetUserTeamsUseCase(get()) }
    factory { com.wandr.domain.usecase.GetTeamUseCase(get()) }
    factory { com.wandr.domain.usecase.GetTeamMembersUseCase(get()) }
    factory { com.wandr.domain.usecase.RefreshTeamDetailsUseCase(get()) }
    factory { com.wandr.domain.usecase.UpdateTeamUseCase(get()) }
    factory { com.wandr.domain.usecase.SetTeamImageUseCase(get()) }
    factory { com.wandr.domain.usecase.RemoveTeamImageUseCase(get()) }
    factory { com.wandr.domain.usecase.RefreshUserTeamsUseCase(get()) }
    factory { JoinTeamViaInviteUseCase(get()) }
    factory { CreateChallengeUseCase(get()) }
    factory { com.wandr.domain.usecase.UpdateChallengeUseCase(get()) }
    factory { com.wandr.domain.usecase.GetChallengesUseCase(get()) }
    factory { com.wandr.domain.usecase.GetChallengeUseCase(get()) }
    factory { com.wandr.domain.usecase.GetChallengeParticipationsUseCase(get()) }
    factory { com.wandr.domain.usecase.LeaveChallengeUseCase(get()) }
    factory { com.wandr.domain.usecase.WithdrawTeamFromChallengeUseCase(get()) }
    factory { com.wandr.domain.usecase.SetChallengeCoverUseCase(get()) }
    factory { com.wandr.domain.usecase.RemoveChallengeCoverUseCase(get()) }
    factory { com.wandr.domain.usecase.GetTeamStandingsUseCase(get()) }
    factory { com.wandr.domain.usecase.EnrollTeamInChallengeUseCase(get()) }
    factory { com.wandr.domain.usecase.RefreshChallengesUseCase(get()) }
    factory { GetChallengeLeaderboardUseCase(get()) }
    factory { JoinChallengeUseCase(get()) }
    factory { EvaluateChallengeStatusUseCase() }
    factory { com.wandr.domain.usecase.CreateManualActivityUseCase(get()) }
    factory { com.wandr.domain.usecase.RecordGpsActivityUseCase(get()) }
    factory { com.wandr.domain.usecase.GetUserActivitiesUseCase(get()) }
    factory { com.wandr.domain.usecase.GetActivityUseCase(get()) }
    factory { com.wandr.domain.usecase.GetActivityTrackUseCase(get()) }
    factory { com.wandr.domain.usecase.UpdateActivityUseCase(get()) }
    factory { com.wandr.domain.usecase.GetUserActivityCountUseCase(get()) }
    factory { com.wandr.domain.usecase.GetTeamActivitiesUseCase(get()) }
    factory { com.wandr.domain.usecase.DeleteActivityUseCase(get()) }
}
