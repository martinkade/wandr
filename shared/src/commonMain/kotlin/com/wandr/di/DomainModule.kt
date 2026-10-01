package com.wandr.di

import com.wandr.domain.usecase.CreateChallengeUseCase
import com.wandr.domain.usecase.CreateTeamUseCase
import com.wandr.domain.usecase.EvaluateChallengeStatusUseCase
import com.wandr.domain.usecase.GetChallengeLeaderboardUseCase
import com.wandr.domain.usecase.GetProfileUseCase
import com.wandr.domain.usecase.GetTeamChallengesUseCase
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
    factory { LoginUseCase(get()) }
    factory { RegisterUseCase(get()) }
    factory { LogoutUseCase(get()) }
    factory { GetProfileUseCase(get()) }
    factory { UpdateProfileUseCase(get()) }
    factory { UploadAvatarUseCase(get()) }
    factory { CreateTeamUseCase(get()) }
    factory { GetUserTeamsUseCase(get()) }
    factory { JoinTeamViaInviteUseCase(get()) }
    factory { CreateChallengeUseCase(get()) }
    factory { GetTeamChallengesUseCase(get()) }
    factory { GetChallengeLeaderboardUseCase(get()) }
    factory { JoinChallengeUseCase(get()) }
    factory { EvaluateChallengeStatusUseCase() }
}
