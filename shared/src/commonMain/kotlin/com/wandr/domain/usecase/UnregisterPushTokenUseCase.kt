package com.wandr.domain.usecase

import com.wandr.domain.repository.PushTokenRepository

class UnregisterPushTokenUseCase(private val repository: PushTokenRepository) {
    suspend operator fun invoke(token: String) = repository.unregister(token)
}
