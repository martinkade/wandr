package com.wandr.domain.usecase

import com.wandr.domain.model.Challenge
import com.wandr.domain.repository.ChallengeRepository
import kotlinx.coroutines.flow.Flow

/** All challenges visible to the signed-in user: open ones and group ones teams can enroll in. */
class GetChallengesUseCase(private val challengeRepository: ChallengeRepository) {
    operator fun invoke(): Flow<List<Challenge>> = challengeRepository.getChallenges()
}
